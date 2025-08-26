package ict.project.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
public class BackfillAcceptedEssaysService {

    private final WebClient chroma;

    // ✅ 명시적 생성자 + Qualifier로 WebClient 충돌 해결
    @Autowired
    public BackfillAcceptedEssaysService(@Qualifier("chromaWebClient") WebClient chroma) {
        this.chroma = chroma;
    }

    // application.properties 예:
    // chroma.tenant=default_tenant
    // chroma.database=default_db
    private final String tenant = System.getProperty("chroma.tenant", "default_tenant");
    private final String database = System.getProperty("chroma.database", "default_db");

    /**
     * 컬렉션을 페이지 단위로 가져와 일괄 처리(backfill)
     */
    public void runBackfill(String collection, int pageSize, int maxDocs) {
        Objects.requireNonNull(collection, "collection must not be null");
        if (pageSize <= 0) pageSize = 100;

        UUID cid;
        try {
            cid = UUID.fromString(collection);
        } catch (Exception ignore) {
            cid = null; // 문자열 이름 컬렉션일 수도 있음
        }
        String collectionPath = (cid != null) ? cid.toString() : collection;

        int offset = 0;
        int processed = 0;

        log.info("Backfill start: collection='{}' (path={}), pageSize={}, maxDocs={}",
                collection, collectionPath, pageSize, maxDocs);

        while (true) {
            // ✅ 더블브레이스 초기화 금지 (offset 캡처 버그 방지)
            Map<String, Object> payload = new HashMap<>();
            payload.put("limit", pageSize);
            payload.put("offset", offset);
            payload.put("include", List.of("documents")); // ids 금지, where/where_document 미지정

            Map<String, Object> pageResp = chroma.post()
                    .uri("/tenants/{t}/databases/{d}/collections/{cid}/get", tenant, database, collectionPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).map(body -> {
                                log.error("Chroma GET error {} body={}", resp.statusCode(), body);
                                return new RuntimeException("Chroma get failed: " + body);
                            })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            List<String> docs = extractDocs(pageResp);
            if (docs.isEmpty()) {
                log.info("No more documents. offset={}, processed={}", offset, processed);
                break;
            }

            // TODO: 여기서 docs 처리 (파싱/저장/재색인 등)
            processed += docs.size();
            log.info("Fetched {} docs (this page), total processed={}", docs.size(), processed);

            // 다음 페이지
            offset += docs.size();

            if (maxDocs > 0 && processed >= maxDocs) {
                log.info("Reached maxDocs ({}). Stop.", maxDocs);
                break;
            }
        }

        log.info("Backfill done. total processed={}", processed);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractDocs(Map<String, Object> pageResp) {
        if (pageResp == null) return Collections.emptyList();
        Object docsObj = pageResp.getOrDefault("documents", null);
        if (!(docsObj instanceof List<?> list)) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;
            if (o instanceof List<?> inner) {
                for (Object i : inner) if (i != null) out.add(String.valueOf(i));
            } else {
                out.add(String.valueOf(o));
            }
        }
        return out;
    }
}

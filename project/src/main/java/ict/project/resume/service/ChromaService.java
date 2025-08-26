package ict.project.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChromaService {

    private final WebClient chroma;

    @Value("${chroma.tenant:default_tenant}")
    private String tenant;

    @Value("${chroma.database:default_db}")
    private String database;

    public ChromaService(@Qualifier("chromaWebClient") WebClient chroma) {
        this.chroma = chroma;
    }

    /* =======================
     * Collections
     * ======================= */

    /** 컬렉션 정보 조회 (404면 존재하지 않음) */
    public Map<String, Object> getCollectionInfo(String collectionId) {
        return chroma.get()
                .uri("/tenants/{t}/databases/{d}/collections/{cid}", tenant, database, collectionId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            log.warn("get collection info error {} body={}", resp.statusCode(), body);
                            return new RuntimeException("get collection " + resp.statusCode() + ": " + body);
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    /* =======================
     * Write (add/upsert)
     * ======================= */

    /**
     * 단건 upsert 유사 동작 (v2 API에선 보통 /add 사용)
     * - 기존 id가 있으면 교체/갱신이 아닌 append 동작일 수 있으므로,
     *   진짜 upsert가 필요하면 사전에 delete 후 add 하는 로직을 별도로 구현하세요.
     */
    public void upsert(String collectionId,
                       String id,
                       String document,
                       List<Float> embedding,
                       Map<String, Object> metadata) {

        Map<String, Object> payload = Map.of(
                "ids", Collections.singletonList(id),
                "documents", Collections.singletonList(document),
                "metadatas", Collections.singletonList(metadata == null ? Map.of() : metadata),
                "embeddings", Collections.singletonList(embedding == null ? List.of() : embedding)
        );

        chroma.post()
                .uri("/tenants/{t}/databases/{d}/collections/{cid}/add", tenant, database, collectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            log.error("Chroma add error {} body={}", resp.statusCode(), body);
                            return new RuntimeException("Chroma add failed: " + body);
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    /* =======================
     * Read (get by page)
     * ======================= */

    /**
     * 페이지네이션 조회
     * - where / where_document는 필요할 때만 보냄 (빈 객체 금지)
     */
    public Map<String, Object> getByPage(String collectionId, int limit, int offset) {
        Map<String, Object> payload = Map.of(
                "limit", limit,
                "offset", offset,
                "include", List.of("documents", "metadatas", "distances")
        );

        return chroma.post()
                .uri("/tenants/{t}/databases/{d}/collections/{cid}/get", tenant, database, collectionId)
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
    }

    /* =======================
     * Query
     * ======================= */

    /**
     * 코사인 기반 유사도 질의
     */
    public Map<String, Object> query(String collectionId, List<Float> queryEmbedding, int topK) {
        Map<String, Object> payload = Map.of(
                "query_embeddings", List.of(queryEmbedding == null ? List.of() : queryEmbedding),
                "n_results", topK,
                "include", List.of("documents", "metadatas", "distances")
        );

        return chroma.post()
                .uri("/tenants/{t}/databases/{d}/collections/{cid}/query", tenant, database, collectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body -> {
                            log.error("Chroma query error {} body={}", resp.statusCode(), body);
                            return new RuntimeException("Chroma query failed: " + body);
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
    // ChromaService.java 에 추가
// 상단 import에 추가

    /** 컬렉션 이름으로 조회 후 query 실행 (없으면 생성) */
    public Map<String, Object> queryByName(String collectionName, List<Float> queryEmbedding, int topK) {
        String cid = ensureCollectionId(collectionName);
        return query(cid, queryEmbedding, topK);
    }

    /** 컬렉션 이름으로 id 확보 (없으면 생성) */
    public String ensureCollectionId(String collectionName) {
        // 1) 이름으로 get
        String id = chroma.post()
                .uri("/tenants/{t}/databases/{d}/collections/get", tenant, database)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", collectionName))
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .map(map -> (String) map.get("id"));
                    } else if (resp.statusCode().value() == 404) {
                        return Mono.just(null); // 없으면 null
                    } else {
                        return resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("get collection by name failed: " + body)));
                    }
                })
                .block();

        if (id != null) return id;

        // 2) 없으면 생성
        Map<String, Object> created = chroma.post()
                .uri("/tenants/{t}/databases/{d}/collections", tenant, database)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", collectionName))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body -> new RuntimeException("create collection failed: " + body))
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return (String) created.get("id");
    }

}

//package ict.project.resume.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//import java.util.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ChromaService {
//    private final @Qualifier("chromaWebClient") WebClient chroma;
//
//    @Value("${chroma.base-url}")  String baseUrl;      // 예: http://localhost:8000/api/v2
//    @Value("${chroma.tenant}")    String tenant;       // 예: default_tenant
//    @Value("${chroma.database}")  String database;     // 예: default_db
//
//    /** 컬렉션 이름으로 id 조회 없으면 생성 */
//    public String ensureCollectionId(String name) {
//        // 1) 목록 조회 → 이름 매칭
//        var list = chroma.get().uri(uri -> uri
//                        .path("/tenants/{t}/databases/{d}/collections")
//                        .queryParam("limit", 1000).build(tenant, database))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<List<Map<String,Object>>>(){})
//                .block();
//
//        if (list != null) {
//            for (var m : list) {
//                if (name.equals(String.valueOf(m.get("name")))) {
//                    return String.valueOf(m.get("id"));
//                }
//            }
//        }
//
//        // 2) 생성
//        Map<String,Object> created = chroma.post()
//                .uri("/tenants/{t}/databases/{d}/collections", tenant, database)
//                .bodyValue(Map.of("name", name, "get_or_create", true))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
//                .block();
//
//        if (created == null || created.get("id") == null)
//            throw new IllegalStateException("컬렉션 생성 실패: " + name);
//
//        return String.valueOf(created.get("id"));
//    }
//
//    /** 업서트 */
//    public void upsert(String collectionName, String docId, String document,
//                       List<Float> embedding, Map<String,Object> metadata) {
//        String cid = ensureCollectionId(collectionName);
//
//        Map<String,Object> payload = Map.of(
//                "ids",        List.of(docId),
//                "documents",  List.of(document),
//                "metadatas",  List.of(metadata == null ? Map.of() : metadata),
//                "embeddings", List.of(embedding)
//        );
//
//        chroma.post()
//                .uri("/tenants/{t}/databases/{d}/collections/{cid}/upsert",
//                        tenant, database, cid)
//                .bodyValue(payload)
//                .retrieve()
//                .toBodilessEntity()
//                .block();
//    }
//
//    /** 쿼리 */
//    public List<Map<String,Object>> query(String collectionName,
//                                          List<Float> queryEmbedding, int topK) {
//        String cid = ensureCollectionId(collectionName);
//
//        Map<String,Object> payload = Map.of(
//                "query_embeddings", List.of(queryEmbedding),
//                "n_results", topK,
//                "include", List.of("documents","metadatas","distances","ids")
//        );
//
//        Map<String,Object> res = chroma.post()
//                .uri("/tenants/{t}/databases/{d}/collections/{cid}/query",
//                        tenant, database, cid)
//                .bodyValue(payload)
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>(){})
//                .block();
//
//        // 응답 평탄화 (기존과 유사)
//        List<List<String>> ids   = cast2D(res.get("ids"));
//        List<List<String>> docs  = cast2D(res.get("documents"));
//        List<List<Map<String,Object>>> metas = cast2D(res.get("metadatas"));
//
//        List<Map<String,Object>> out = new ArrayList<>();
//        if (ids != null && !ids.isEmpty()) {
//            for (int i = 0; i < ids.get(0).size(); i++) {
//                out.add(Map.of(
//                        "id",       ids.get(0).get(i),
//                        "document", (docs==null||docs.isEmpty())? null : docs.get(0).get(i),
//                        "metadata", (metas==null||metas.isEmpty())? Map.of() : metas.get(0).get(i)
//                ));
//            }
//        }
//        return out;
//    }
//
//    @SuppressWarnings("unchecked")
//    private static <T> List<List<T>> cast2D(Object o){ return (List<List<T>>) o; }
//
//
//}
//

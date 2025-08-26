package ict.project.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class ChromaVectorStoreService {

    /** 컨트롤러에서 import 해서 쓰는 중첩 타입 */
    public static record SearchHit(String id, String text, double distance) {}

    private final WebClient chroma;

    // 👇 명시적 생성자 + Qualifier 로 어떤 빈을 쓸지 확정
    public ChromaVectorStoreService(@Qualifier("chromaWebClient") WebClient chroma) {
        this.chroma = chroma;
    }

    /** 컬렉션에서 임베딩으로 topK 검색 */
    public List<SearchHit> search(String collection, List<Float> embedding, int topK) {
        try {
            Map<String, Object> payload = Map.of(
                    "query_embeddings", List.of(embedding),
                    "n", topK,
                    "include", List.of("ids", "documents", "distances")
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = chroma.post()
                    .uri("/api/v1/collections/{name}/query", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(e -> {
                        log.error("Chroma query failed", e);
                        return Mono.just(Map.of());
                    })
                    .block();

            List<SearchHit> out = new ArrayList<>();
            if (body == null) return out;

            List<List<String>> ids = as2DStringList(body.get("ids"));
            List<List<String>> docs = as2DStringList(body.get("documents"));
            List<List<Double>> dists = as2DDoubleList(body.get("distances"));

            if (!ids.isEmpty()) {
                List<String> rowIds = ids.get(0);
                List<String> rowDocs = (!docs.isEmpty() ? docs.get(0) : List.of());
                List<Double> rowDists = (!dists.isEmpty() ? dists.get(0) : List.of());

                int n = rowIds.size();
                for (int i = 0; i < n; i++) {
                    String id = safeGet(rowIds, i, "");
                    String text = safeGet(rowDocs, i, "");
                    double dist = safeGet(rowDists, i, Double.MAX_VALUE);
                    out.add(new SearchHit(id, text, dist));
                }
            }
            return out;
        } catch (Exception e) {
            log.error("Chroma search error", e);
            return List.of();
        }
    }

    // -------- helpers --------
    @SuppressWarnings("unchecked")
    private static List<List<String>> as2DStringList(Object v) {
        if (v instanceof List<?> l) return (List<List<String>>) (List<?>) l;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<List<Double>> as2DDoubleList(Object v) {
        if (v instanceof List<?> l) return (List<List<Double>>) (List<?>) l;
        return List.of();
    }

    private static <T> T safeGet(List<T> list, int idx, T def) {
        return (list != null && idx >= 0 && idx < list.size()) ? list.get(idx) : def;
    }
}



//package ict.project.resume.service;
//
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.*;
//
///**
// * VectorStoreService 구현체 (Chroma v2 "frontend" REST API)
// *
// * application.properties 예시:
// *   chroma.base-url=http://localhost:8000/api/v2
// *   chroma.tenant=default_tenant
// *   chroma.database=default_db
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ChromaVectorStoreService implements VectorStoreService {
//
//    @Value("${chroma.base-url}")
//    private String baseUrl;
//
//    @Value("${chroma.tenant:default_tenant}")
//    private String tenant;
//
//    @Value("${chroma.database:default_db}")
//    private String database;
//
//    /** ChromaClientConfig에서 만든 WebClient 빈 */
//    private final @Qualifier("chromaWebClient") WebClient chroma;
//
//    /** name -> collection_id 캐시 */
//    private final Map<String, String> collectionIdCache = new HashMap<>();
//
//    @PostConstruct
//    void init() {
//        ensureTenantAndDatabase();
//    }
//
//    /** 서버/테넌트/DB 보장 */
//    private void ensureTenantAndDatabase() {
//        // optional: 서버 살아있는지
//        try {
//            chroma.get().uri(baseUrl + "/healthcheck").retrieve().toBodilessEntity().block();
//        } catch (Exception ignore) {}
//
//        // tenant 만들기 (이미 있으면 통과)
//        try {
//            chroma.post()
//                    .uri(baseUrl + "/tenants")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .bodyValue(Map.of("name", tenant))
//                    .retrieve()
//                    .toBodilessEntity()
//                    .block();
//        } catch (Exception ignore) {}
//
//        // database 만들기 (이미 있으면 통과)
//        try {
//            chroma.post()
//                    .uri(baseUrl + "/tenants/{t}/databases", tenant)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .bodyValue(Map.of("name", database))
//                    .retrieve()
//                    .toBodilessEntity()
//                    .block();
//        } catch (Exception ignore) {}
//    }
//
//    @Override
//    public void ensureCollection(String name) {
//        ensureCollectionId(name);
//    }
//
//    /** 컬렉션 id 보장 (없으면 get_or_create로 생성) */
//    private String ensureCollectionId(String name) {
//        if (collectionIdCache.containsKey(name)) return collectionIdCache.get(name);
//
//        // 목록에서 찾기
//        List<Map<String, Object>> list = chroma.get()
//                .uri(baseUrl + "/tenants/{t}/databases/{d}/collections?limit=1000&offset=0", tenant, database)
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, resp ->
//                        resp.bodyToMono(String.class).map(body ->
//                                new RuntimeException("list collections " + resp.statusCode() + ": " + body)))
//                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
//                .block();
//
//        if (list != null) {
//            for (var m : list) {
//                if (name.equals(String.valueOf(m.get("name")))) {
//                    String id = String.valueOf(m.get("id"));
//                    collectionIdCache.put(name, id);
//                    return id;
//                }
//            }
//        }
//
//        // 없으면 생성
//        Map<String, Object> created = chroma.post()
//                .uri(baseUrl + "/tenants/{t}/databases/{d}/collections", tenant, database)
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(Map.of("name", name, "get_or_create", true))
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, resp ->
//                        resp.bodyToMono(String.class).map(body ->
//                                new RuntimeException("create collection " + resp.statusCode() + ": " + body)))
//                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                .block();
//
//        if (created == null || created.get("id") == null) {
//            throw new IllegalStateException("컬렉션 생성 실패: " + name);
//        }
//        String id = String.valueOf(created.get("id"));
//        collectionIdCache.put(name, id);
//        return id;
//    }
//
//    @Override
//    public void upsert(String collection, String id, float[] vector, Map<String, Object> metadata, String text) {
//        String cid = ensureCollectionId(collection);
//
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("ids", List.of(id));
//        payload.put("embeddings", List.of(toFloatList(vector))); // 2D
//        payload.put("documents", List.of(text));
//        payload.put("metadatas", List.of(metadata == null ? Map.of() : metadata));
//
//        chroma.post()
//                .uri(baseUrl + "/tenants/{t}/databases/{d}/collections/{cid}/upsert", tenant, database, cid)
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(payload)
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, resp ->
//                        resp.bodyToMono(String.class).map(body ->
//                                new RuntimeException("upsert " + resp.statusCode() + ": " + body)))
//                .toBodilessEntity()
//                .block();
//    }
//
//    @Override
//    public List<SearchResult> query(String collection, float[] vector, int topK) {
//        String cid = ensureCollectionId(collection);
//
//        // (선택) 컬렉션 차원과 쿼리 벡터 차원 일치 확인
//        try {
//            Map<String, Object> info = chroma.get()
//                    .uri(baseUrl + "/tenants/{t}/databases/{d}/collections/{cid}", tenant, database, cid)
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                    .block();
//            Integer dimension = (info != null && info.get("dimension") instanceof Number)
//                    ? ((Number) info.get("dimension")).intValue() : null;
//            if (dimension != null && dimension != vector.length) {
//                throw new IllegalArgumentException(
//                        "Embedding dimension mismatch. collection=" + dimension + ", query=" + vector.length);
//            }
//        } catch (Exception e) {
//            log.debug("dimension check skipped/failed: {}", e.getMessage());
//        }
//
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("query_embeddings", List.of(toFloatList(vector))); // 2D
//        payload.put("n_results", Math.max(1, topK));
//        payload.put("include", List.of("documents", "metadatas", "distances"));
//
//        Map<String, Object> res = chroma.post()
//                .uri(baseUrl + "/tenants/{t}/databases/{d}/collections/{cid}/query", tenant, database, cid)
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(payload)
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, resp ->
//                        resp.bodyToMono(String.class).map(body ->
//                                new RuntimeException("query " + resp.statusCode() + ": " + body)))
//                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                .block();
//
//        if (res == null) return List.of();
//
//        @SuppressWarnings("unchecked")
//        List<List<String>> ids = (List<List<String>>) res.get("ids");
//        @SuppressWarnings("unchecked")
//        List<List<Double>> distances = (List<List<Double>>) res.get("distances");
//        @SuppressWarnings("unchecked")
//        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) res.get("metadatas");
//        @SuppressWarnings("unchecked")
//        List<List<String>> documents = (List<List<String>>) res.get("documents");
//
//        if (ids == null || ids.isEmpty()) return List.of();
//
//        int n = ids.get(0).size();
//        List<SearchResult> out = new ArrayList<>(n);
//        for (int i = 0; i < n; i++) {
//            String _id = ids.get(0).get(i);
//            Double _dist = (distances != null && !distances.isEmpty()) ? distances.get(0).get(i) : null;
//            Map<String, Object> _meta = (metadatas != null && !metadatas.isEmpty()) ? metadatas.get(0).get(i) : Map.of();
//            String _doc = (documents != null && !documents.isEmpty()) ? documents.get(0).get(i) : null;
//            out.add(new SearchResult(_id, _dist == null ? Double.NaN : _dist, _meta, _doc));
//        }
//        return out;
//    }
//
//    /* ------------ utils ------------ */
//
//    private static List<Float> toFloatList(float[] v) {
//        if (v == null) return List.of();
//        List<Float> out = new ArrayList<>(v.length);
//        for (float f : v) out.add(f);
//        return out;
//    }
//}


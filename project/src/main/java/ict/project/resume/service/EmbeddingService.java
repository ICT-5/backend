// src/main/java/ict/project/resume/service/EmbeddingService.java
package ict.project.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final OpenAiEmbeddingClient openAiEmbeddingClient;

    @Value("${openai.embeddings.model:text-embedding-3-small}")
    private String model;

    /** 단일 텍스트 임베딩 -> float[] */
    public float[] embed(String text) {
        String safe = limitForEmbedding(nz(text));
        if (safe.isBlank()) return new float[0];

        try {
            List<String> inputs = List.of(safe);     // <-- List<String>
            List<float[]> result = openAiEmbeddingClient.embed(model, inputs); // <-- 시그니처 일치
            return (result != null && !result.isEmpty()) ? result.get(0) : new float[0];
        } catch (Exception e) {
            log.error("OpenAI embeddings error", e);
            throw new RuntimeException("OpenAI embeddings failed: " + e.getMessage(), e);
        }
    }

    /** float[] -> List<Float> 가 필요한 곳(예: Chroma)용 */
    public List<Float> embedAsList(String text) {
        float[] arr = embed(text);
        List<Float> out = new ArrayList<>(arr.length);
        for (float v : arr) out.add(v);
        return out;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** 너무 긴 입력을 잘라서 모델 한도 보호 */
    private static String limitForEmbedding(String s) {
        int maxChars = 7000; // 필요시 조정 (토큰 기준이 더 정확하지만, 문자수 컷도 실무에선 충분)
        return (s.length() > maxChars) ? s.substring(0, maxChars) : s;
    }
}


//package ict.project.resume.service;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
///**
// * 문자열 → 임베딩 벡터 변환.
// * - OpenAI API 키가 있으면 실제 임베딩 호출
// * - 없으면 개발용 더미 임베딩(결정적 난수)로 동작
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class EmbeddingService {
//
//    @Value("${openai.apiKey:}")
//    private String apiKey;
//
//    @Value("${openai.embeddingModel:text-embedding-3-small}")
//    private String model;
//
//    @Value("${openai.embeddings.endpoint:https://api.openai.com/v1/embeddings}")
//    private String endpoint;
//
//    @Value("${openai.requestTimeoutSec:30}")
//    private int timeoutSec;
//
//    private final HttpClient http = HttpClient.newBuilder()
//            .connectTimeout(Duration.ofSeconds(10))
//            .build();
//
//    private final ObjectMapper mapper = new ObjectMapper()
//            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//    /** 단건 임베딩 (BulkExcelIngestService에서 사용) */
//    public float[] embed(String text) throws Exception {
//        if (text == null) text = "";
//        if (apiKey == null || apiKey.isBlank()) {
//            // ✅ 개발/테스트용: 결정적 더미 임베딩
//            return dummyEmbedding(text, 384);
//        }
//        // ✅ 실제 OpenAI 임베딩 호출
//        return callOpenAIEmbedding(text);
//    }
//
//    /* ======================= 내부 구현 ======================= */
//
//    private float[] callOpenAIEmbedding(String input) throws Exception {
//        EmbReq body = new EmbReq(model, input);
//        String json = mapper.writeValueAsString(body);
//
//        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
//                .timeout(Duration.ofSeconds(Math.max(5, timeoutSec)))
//                .header("Content-Type", "application/json")
//                .header("Authorization", "Bearer " + apiKey)
//                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
//                .build();
//
//        HttpResponse<String> res =
//                http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
//
//        if (res.statusCode() / 100 != 2) {
//            String bodyTxt = res.body();
//            log.warn("Embedding API failed: status={}, body={}", res.statusCode(), bodyTxt);
//            throw new RuntimeException("Embedding API 호출 실패 (status=" + res.statusCode() + ")");
//        }
//
//        EmbRes parsed = mapper.readValue(res.body(), EmbRes.class);
//        if (parsed == null || parsed.data == null || parsed.data.isEmpty()
//                || parsed.data.get(0).embedding == null) {
//            throw new RuntimeException("Embedding 응답 파싱 실패");
//        }
//
//        List<Double> dbl = parsed.data.get(0).embedding;
//        float[] out = new float[dbl.size()];
//        for (int i = 0; i < dbl.size(); i++) out[i] = dbl.get(i).floatValue();
//        l2Normalize(out);
//        return out;
//    }
//
//    /** API 키가 없을 때 쓰는 결정적 더미 임베딩(고정 차원, L2 정규화) */
//    private float[] dummyEmbedding(String text, int dim) {
//        Random rnd = new Random(text.hashCode());
//        float[] v = new float[dim];
//        for (int i = 0; i < dim; i++) {
//            v[i] = (rnd.nextFloat() * 2f - 1f); // [-1,1]
//        }
//        l2Normalize(v);
//        return v;
//    }
//
//    private void l2Normalize(float[] v) {
//        double sum = 0.0;
//        for (float x : v) sum += (double) x * x;
//        double norm = Math.sqrt(Math.max(sum, 1e-12));
//        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
//    }
//
//    /* ===== DTOs for OpenAI ===== */
//    @Data
//    static class EmbReq {
//        public String model;
//        public String input;
//
//        EmbReq(String model, String input) {
//            this.model = model;
//            this.input = input;
//        }
//    }
//
//    @Data
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    static class EmbRes {
//        public List<Item> data = new ArrayList<>();
//
//        @Data
//        @JsonIgnoreProperties(ignoreUnknown = true)
//        static class Item {
//            public String object;
//            public List<Double> embedding;
//            public int index;
//        }
//    }
//}

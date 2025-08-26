//// src/main/java/ict/project/resume/service/OpenAiEmbeddingClientHttp.java
//package ict.project.resume.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//import reactor.util.retry.Retry;
//
//import java.io.IOException;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeoutException;
//
//// OpenAiEmbeddingClientHttp.java
//@Slf4j
//@Service
//public class OpenAiEmbeddingClientHttp {
//    private final WebClient openai;
//    @Value("${openai.embeddingModel:text-embedding-3-small}")
//    private String model;
//
//    public OpenAiEmbeddingClientHttp(@Qualifier("openaiWebClient") WebClient openai) {
//        this.openai = openai; // baseUrl = https://api.openai.com/v1  (uri에는 /v1 붙이지 않음)
//    }
//
//    public List<float[]> embed(List<String> inputs) {
//        Map<String, Object> body = Map.of("model", model, "input", inputs);
//
//        Map<?,?> res = openai.post()
//                .uri("/embeddings")
//                .bodyValue(body)
//                .exchangeToMono(cr -> {
//                    if (cr.statusCode().is2xxSuccessful()) {
//                        return cr.bodyToMono(Map.class);
//                    }
//                    return cr.bodyToMono(String.class).flatMap(msg ->
//                            Mono.error(new RuntimeException(cr.statusCode() + " : " + msg)));
//                })
//                // ⏱ 타임아웃
//                .timeout(Duration.ofSeconds(40))
//                // 🔁 429/5xx/타임아웃/IO 재시도 (지수 백오프)
//                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
//                        .maxBackoff(Duration.ofSeconds(20))
//                        .jitter(0.2)
//                        .filter(this::isRetryable))
//                .block();
//
//        if (res == null) return List.of();
//
//        // {"data":[{"embedding":[...]}...]}
//        List<Map<String,Object>> data = (List<Map<String,Object>>) res.get("data");
//        if (data == null) return List.of();
//
//        List<float[]> out = new ArrayList<>(data.size());
//        for (Map<String,Object> d : data) {
//            List<Number> arr = (List<Number>) d.get("embedding");
//            float[] f = new float[arr.size()];
//            for (int i = 0; i < arr.size(); i++) f[i] = arr.get(i).floatValue();
//            out.add(f);
//        }
//        return out;
//    }
//
//    private boolean isRetryable(Throwable t) {
//        if (t instanceof WebClientResponseException w) {
//            int s = w.getStatusCode().value();
//            return s == 429 || s == 500 || s == 502 || s == 503 || s == 504;
//        }
//        return t instanceof TimeoutException || t instanceof IOException;
//    }
//}


// src/main/java/ict/project/resume/service/OpenAiEmbeddingClientHttp.java
package ict.project.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// OpenAiEmbeddingClientHttp.java
@Service
public class OpenAiEmbeddingClientHttp implements OpenAiEmbeddingClient {

    private final WebClient openai;   // 오로지 이 한 개만!
    private final String model;

    public OpenAiEmbeddingClientHttp(
            @Qualifier("openaiWebClient") WebClient openai,
            @Value("${openai.embeddings.model:text-embedding-3-small}") String model
    ) {
        this.openai = openai;
        this.model = model;
    }

    @Override
    public List<float[]> embed(String modelOverride, List<String> inputs) {
        String useModel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride : this.model;

        Map<String, Object> req = Map.of(
                "model", useModel,
                "input", inputs
        );

        Map<String, Object> resp = openai.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // data: List<Map<String, Object>>
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");

        List<float[]> out = new ArrayList<>();
        for (Map<String, Object> d : data) {
            // ❌ (Map<String,Object>) d.get("embedding")  -> 잘못된 캐스팅
            @SuppressWarnings("unchecked")
            List<Number> emb = (List<Number>) d.get("embedding"); // ✅ 배열로 받기
            float[] vec = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                vec[i] = emb.get(i).floatValue();
            }
            out.add(vec);
        }
        return out;
    }

}

package ict.project.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmClientService {

    private final WebClient openai;

    // ✅ 명시적으로 openaiWebClient를 받도록 지정
    public LlmClientService(@Qualifier("openaiWebClient") WebClient openai) {
        this.openai = openai;
    }

    @Value("${openai.chatModel:gpt-4o-mini}")
    private String chatModel;

    public String complete(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", chatModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a helpful recruiting specialist."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2
            );

            Mono<Map> call = openai.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<?, ?> res = call.block();
            if (res == null) return "";

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) res.get("choices");
            if (choices == null || choices.isEmpty()) return "";

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return "";

            Object content = message.get("content");
            return content == null ? "" : content.toString();

        } catch (Exception e) {
            log.error("LLM complete error: {}", e.getMessage(), e);
            return "(분석 생성 실패: " + e.getMessage() + ")";
        }
    }
}

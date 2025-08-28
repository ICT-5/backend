package ict.project.feedback.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ict.project.feedback.core.LlmClient;
import ict.project.feedback.core.LlmRewrite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "llm", name = "mode", havingValue = "openai")
public class OpenAiLikeClient implements LlmClient {

    @Value("${llm.endpoint}")         // 예: https://api.openai.com/v1  또는  https://api.openai.com/v1/chat/completions
    private String endpoint;

    @Value("${llm.apiKey}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.temperature:0.2}")
    private double temperature;

    @Value("${llm.maxTokens:300}")
    private int maxTokens;

    @Value("${llm.timeoutMs:6000}")
    private long timeoutMs;

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public LlmRewrite rewrite(String systemPrompt, String userPrompt) {
        String url = normalizeEndpoint(endpoint); // base든 full이든 OK

        String payload = """
        {
          "model": %s,
          "messages": [
            {"role":"system","content":%s},
            {"role":"user","content":%s}
          ],
          "response_format": {"type":"json_object"},
          "temperature": %s,
          "max_tokens": %s
        }
        """.formatted(q(model), q(systemPrompt), q(userPrompt), temperature, maxTokens);

        try {
            String resp = WebClient.create(url)
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(apiKey))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.max(1))
                    .block();

            if (resp == null || resp.isBlank()) {
                return new LlmRewrite("", List.of());
            }

            JsonNode root = om.readTree(resp);
            String content = root.at("/choices/0/message/content").asText("{}");

            // 모델이 JSON만 반환하도록 시킨 상태라 바로 파싱 시도
            JsonNode node = om.readTree(content);
            String rewrite = node.path("rewrite").asText("");
            List<String> jdInsert = om.convertValue(
                    node.path("jdInsert"),
                    om.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return new LlmRewrite(
                    rewrite == null ? "" : rewrite,
                    jdInsert == null ? List.of() : jdInsert
            );

        } catch (Exception e) {
            // 실패 시 안전 폴백
            return new LlmRewrite("", List.of());
        }
    }

    /** endpoint가 base URL이면 /chat/completions 붙여줌 */
    private static String normalizeEndpoint(String ep) {
        if (ep == null || ep.isBlank()) return "https://api.openai.com/v1/chat/completions";
        String trimmed = ep.trim();
        if (trimmed.endsWith("/chat/completions")) return trimmed;
        if (trimmed.endsWith("/v1")) return trimmed + "/chat/completions";
        if (trimmed.endsWith("/v1/")) return trimmed + "chat/completions";
        // 이미 /v1/xxx 다른 경로면 그대로 사용한다고 가정
        return trimmed;
    }

    private static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\\\"")) + "\"";
    }
}

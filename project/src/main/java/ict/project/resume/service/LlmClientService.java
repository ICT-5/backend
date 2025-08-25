



package ict.project.resume.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientService {

    /* ======= 설정 ======= */
    @Value("${openai.apiKey:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.provider:openai}") // openai | azure
    private String provider;

    @Value("${openai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String endpoint;

    @Value("${azure.openai.resource:}")
    private String azureResource;
    @Value("${azure.openai.deployment:}")
    private String azureDeployment;
    @Value("${azure.openai.apiVersion:2024-02-15-preview}")
    private String azureApiVersion;

    @Value("${openai.organization:}")
    private String openaiOrg;
    @Value("${openai.project:}")
    private String openaiProject;

    @Value("${openai.requestTimeoutSec:30}")
    private int requestTimeoutSec;

    /* ======= 재시도(백오프) 파라미터 ======= */
    @Value("${openai.retry.maxRetries:3}")
    private int maxRetries; // 429/5xx에서 재시도 횟수

    @Value("${openai.retry.baseDelayMillis:2000}")
    private long baseDelayMillis; // 초기 백오프

    @Value("${openai.retry.maxDelayMillis:45000}")
    private long maxDelayMillis; // 최대 대기

    /* ======= 컨텍스트 길이 가드 ======= */
    private static final int MODEL_CONTEXT_TOKENS = 128_000; // 모델 컨텍스트 한계
    private static final int OUTPUT_BUDGET_TOKENS = 2_000;   // 응답/여유 토큰
    private static final int INPUT_BUDGET_TOKENS  = MODEL_CONTEXT_TOKENS - OUTPUT_BUDGET_TOKENS;
    private static final int CHARS_PER_TOKEN      = 4;       // 대략 1 token ~= 4 chars(보수적)

    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 외부 진입점 */
    public String generateWithRag(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("openai.apiKey 가 설정되지 않았습니다.");
        }
        try {
            // 입력 길이 가드
            String safePrompt = safetyTrim(prompt);

            String url = buildUrl();
            HttpRequest req = buildRequest(url, safePrompt);

            if (log.isInfoEnabled()) {
                log.info("LLM cfg provider={}, endpoint={}, model={}, azureResource={}, azureDeployment={}, apiVersion={}",
                        provider, endpoint, model, azureResource, azureDeployment, azureApiVersion);
            }

            // === 재시도 포함 호출 ===
            HttpResponse<String> res = sendWithRetry(req);

            int sc   = res.statusCode();
            String body = res.body();
            log.debug("LLM raw response status={}, body={}", sc, body);

            if (sc / 100 != 2) {
                String msg = extractErrorMessage(body);
                log.warn("LLM 요청 실패 status={}, body={}", sc, body);
                throw new RuntimeException("LLM API 호출 실패 (status=" + sc + "): " + msg);
            }

            ChatCompletionsResponse parsed = mapper.readValue(body, ChatCompletionsResponse.class);
            if (parsed == null || parsed.getChoices() == null || parsed.getChoices().isEmpty()
                    || parsed.getChoices().get(0).getMessage() == null) {
                throw new RuntimeException("LLM 응답 파싱 실패");
            }
            return parsed.getChoices().get(0).getMessage().getContent();

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("LLM 호출 중 오류", e);
            throw new RuntimeException("LLM 호출 실패: " + e.getMessage(), e);
        }
    }

    public String chat(String prompt) { // 호환용
        return generateWithRag(prompt);
    }

    /* ======================= 내부 유틸 ======================= */

    private String buildUrl() {
        if ("azure".equalsIgnoreCase(provider)) {
            if (isBlank(azureResource) || isBlank(azureDeployment)) {
                throw new IllegalStateException("Azure 설정(azure.openai.resource / azure.openai.deployment)이 비어있습니다.");
            }
            return "https://" + azureResource
                    + ".openai.azure.com/openai/deployments/" + azureDeployment
                    + "/chat/completions?api-version=" + azureApiVersion;
        }
        return endpoint;
    }

    private HttpRequest buildRequest(String url, String prompt) throws Exception {
        ChatCompletionsRequest body;

        if ("azure".equalsIgnoreCase(provider)) {
            body = ChatCompletionsRequest.builder()
                    .messages(List.of(
                            ChatMessage.of("system", systemPrompt()),
                            ChatMessage.of("user", prompt)
                    ))
                    .temperature(0.3)
                    .max_tokens(800)
                    .build();

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, requestTimeoutSec)))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

        } else {
            body = ChatCompletionsRequest.builder()
                    .model(model)
                    .messages(List.of(
                            ChatMessage.of("system", systemPrompt()),
                            ChatMessage.of("user", prompt)
                    ))
                    .temperature(0.3)
                    .max_tokens(800)
                    .build();

            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, requestTimeoutSec)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8));

            if (!isBlank(openaiOrg))     b.header("OpenAI-Organization", openaiOrg);
            if (!isBlank(openaiProject)) b.header("OpenAI-Project", openaiProject);
            return b.build();
        }
    }

    private String systemPrompt() {
        return """
                당신은 채용 담당자입니다. 아래 지침을 따라 응답하세요.
                - 출력은 한국어로 작성.
                - 형식:
                  1) 강점 (불릿)
                  2) 개선점 (불릿)
                  3) 키워드 매칭 (표 형태: 요구역량 | 이력서 근거 | 개선 제안)
                  4) 요약 (3문장 이내)
                - 개인정보/회사기밀은 생성하지 말 것.
                """;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 입력 프롬프트 길이 가드: 128k 한계를 넘지 않도록 문자 기준 보수적 클리핑 */
    private String safetyTrim(String prompt) {
        int maxChars = Math.max(1, INPUT_BUDGET_TOKENS * CHARS_PER_TOKEN);
        if (prompt == null) return "";
        if (prompt.length() <= maxChars) return prompt;

        int keepHead = Math.min(20_000, maxChars / 5);   // 헤더/지침 보존
        int keepTail = maxChars - keepHead - 1_000;      // 최신부 보존(구분자 여유)
        if (keepTail < 0) keepTail = Math.max(0, maxChars - keepHead);

        String head = prompt.substring(0, Math.min(prompt.length(), keepHead));
        String tail = prompt.substring(Math.max(0, prompt.length() - keepTail));

        String trimmed = head + "\n\n...[TRUNCATED FOR LENGTH]...\n\n" + tail;
        log.debug("safetyTrim applied: originalChars={}, trimmedChars={}, maxChars={}",
                prompt.length(), trimmed.length(), maxChars);
        return trimmed;
    }

    /** 에러 바디에서 메시지 추출 */
    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            var node = mapper.readTree(body);
            String m1 = node.path("error").path("message").asText(null);
            String m2 = node.path("message").asText(null);
            if (m1 != null && !m1.isBlank()) return m1;
            if (m2 != null && !m2.isBlank()) return m2;
        } catch (Exception ignore) {}
        return body;
    }

    /* ======================= 재시도(백오프) 구현 ======================= */

    private static final Pattern SECONDS_IN_MSG = Pattern.compile(
            "(?i)try again in\\s+([0-9]+(?:\\.[0-9]+)?)s");

    private HttpResponse<String> sendWithRetry(HttpRequest req) throws Exception {
        int attempt = 0;
        long delay = baseDelayMillis;

        while (true) {
            attempt++;
            try {
                HttpResponse<String> res =
                        http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                int sc = res.statusCode();
                if (sc == 429 || (sc >= 500 && sc <= 599)) {
                    if (attempt > maxRetries) {
                        log.warn("Max retry exceeded. status={}, attempt={}, body={}", sc, attempt, res.body());
                        return res;
                    }

                    long serverHintMs = computeServerSuggestedDelayMillis(res);
                    long sleepMs = Math.min(
                            Math.max(serverHintMs > 0 ? serverHintMs : withJitter(delay), baseDelayMillis),
                            maxDelayMillis
                    );
                    log.warn("Transient error (status={}). retrying in {} ms (attempt {}/{})",
                            sc, sleepMs, attempt, maxRetries);
                    Thread.sleep(sleepMs);

                    // 지수 백오프 증가 (상한 적용)
                    delay = Math.min(delay * 2, maxDelayMillis);
                    continue;
                }

                return res;
            } catch (java.net.http.HttpTimeoutException te) {
                if (attempt > maxRetries) throw te;
                long sleepMs = Math.min(withJitter(delay), maxDelayMillis);
                log.warn("HTTP timeout. retrying in {} ms (attempt {}/{})", sleepMs, attempt, maxRetries);
                Thread.sleep(sleepMs);
                delay = Math.min(delay * 2, maxDelayMillis);
            } catch (java.io.IOException ioe) {
                if (attempt > maxRetries) throw ioe;
                long sleepMs = Math.min(withJitter(delay), maxDelayMillis);
                log.warn("IO error: {}. retrying in {} ms (attempt {}/{})",
                        ioe.getClass().getSimpleName(), sleepMs, attempt, maxRetries);
                Thread.sleep(sleepMs);
                delay = Math.min(delay * 2, maxDelayMillis);
            }
        }
    }

    /** 서버가 제시하는 재시도 힌트(헤더/본문) 해석 → ms */
    private long computeServerSuggestedDelayMillis(HttpResponse<String> res) {
        // 1) Retry-After 헤더(초 또는 HTTP-date)
        Optional<String> retryAfter = res.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            String v = retryAfter.get().trim();
            try {
                // 숫자(초)로 오는 경우
                double sec = Double.parseDouble(v);
                return (long) Math.ceil(sec * 1000.0);
            } catch (NumberFormatException ignored) {
                // 날짜 형태일 수 있음 → 최소 기본 대기
                return Math.max(baseDelayMillis, 3_000);
            }
        }

        // 2) OpenAI 한정: x-ratelimit-reset-requests / -tokens (초 단위)
        Optional<String> resetReq = res.headers().firstValue("x-ratelimit-reset-requests");
        Optional<String> resetTok = res.headers().firstValue("x-ratelimit-reset-tokens");
        String hint = resetTok.orElseGet(() -> resetReq.orElse(null));
        if (hint != null) {
            try {
                double sec = Double.parseDouble(hint);
                return (long) Math.ceil(sec * 1000.0);
            } catch (NumberFormatException ignored) { /* pass */ }
        }

        // 3) 에러 본문 메시지 내 "Please try again in 34.351s" 패턴
        String body = res.body();
        if (body != null) {
            Matcher m = SECONDS_IN_MSG.matcher(body);
            if (m.find()) {
                try {
                    double sec = Double.parseDouble(m.group(1));
                    return (long) Math.ceil(sec * 1000.0);
                } catch (NumberFormatException ignored) { /* pass */ }
            }
        }

        // 기본값
        return 0L;
    }

    private long withJitter(long base) {
        // 0.8x ~ 1.3x 랜덤 지터
        double factor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.5;
        return (long) Math.max(1, base * factor);
    }

    /* ======================= DTO ======================= */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletionsRequest {
        private String model; // azure에서는 생략 가능
        private List<ChatMessage> messages;
        private Double temperature;
        private Integer max_tokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;

        public static ChatMessage of(String role, String content) {
            return ChatMessage.builder().role(role).content(content).build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletionsResponse {
        private List<Choice> choices;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private int index;
            private ChatMessage message;
            private Object logprobs;
            private String finish_reason;
        }
    }
}



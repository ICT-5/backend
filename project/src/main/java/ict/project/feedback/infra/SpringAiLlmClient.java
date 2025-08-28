// src/main/java/org/example/feedback/infra/SpringAiLlmClient.java
package ict.project.feedback.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.project.feedback.core.LlmClient;
import ict.project.feedback.core.LlmRewrite;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "llm", name = "mode", havingValue = "springai")
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chat;
    private final ObjectMapper om = new ObjectMapper();

    public SpringAiLlmClient(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @Override
    public LlmRewrite rewrite(String systemPrompt, String userPrompt) {
        try {
            var call = chat
                    .prompt()
                    // ★ response-format 강제 해제: 내부 추출기 오류 우회
                    .options(OpenAiChatOptions.builder()
                            .model("gpt-4o-mini")
                            .temperature(0.2)
                            .responseFormat(null)
                            .build())
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call();

            // ★ 자동 매핑 사용 금지: 원문 문자열 확보 후 수동 파싱
            String raw = call.content();
            System.out.println("[SpringAiLlmClient] raw content = " + raw);

            String cleaned = stripFences(raw); // ```json ... ``` 같은 펜스/앞뒤 군더더기 제거
            try {
                return om.readValue(cleaned, LlmRewrite.class);
            } catch (Exception parseFail) {
                System.err.println("[SpringAiLlmClient] JSON parse failed: " + parseFail.getMessage());
                return new LlmRewrite("", List.of()); // 안전 기본값
            }

        } catch (RestClientResponseException e) {
            // ★ OpenAI가 401/429/400 등 에러 JSON을 줄 때 원문 확인
            System.err.println("[SpringAiLlmClient] HTTP " + e.getRawStatusCode()
                    + " body=" + e.getResponseBodyAsString());
            return new LlmRewrite("", List.of());

        } catch (Exception e) {
            // ★ 추출 실패 등 기타 예외
            e.printStackTrace();
            System.err.println("[SpringAiLlmClient] LLM call failed: " + e.getMessage());
            return new LlmRewrite("", List.of());
        }
    }

    /** ```json ... ``` / 설명문 제거하고 JSON 본문만 추출 */
    private String stripFences(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
            if (t.endsWith("```")) t = t.substring(0, t.lastIndexOf("```"));
        }
        int i = t.indexOf('{');
        int j = t.lastIndexOf('}');
        if (i >= 0 && j >= i) t = t.substring(i, j + 1);
        return t.trim();
    }
}

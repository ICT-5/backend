package ict.project.feedback.infra;

import ict.project.feedback.core.LlmClient;
import ict.project.feedback.core.LlmRewrite;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * llm.mode=fake 일 때 활성화되는 가짜 LLM 구현체.
 * 실제 호출 없이 즉시 더미 결과를 반환합니다.
 */

@Component
@ConditionalOnProperty(prefix = "llm", name = "mode", havingValue = "fake")
public class FakeLlmClient implements LlmClient {
    @Override
    public LlmRewrite rewrite(String sys, String usr) {
        return new LlmRewrite("FAKE", java.util.List.of("fake"));
    }
}


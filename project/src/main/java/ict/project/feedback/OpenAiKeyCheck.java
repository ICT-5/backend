package ict.project.feedback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class OpenAiKeyCheck {
    @Value("${spring.ai.openai.api-key:}")
    String resolvedKey;

    @PostConstruct
    void check() {
        System.out.println("[OpenAI] resolved key len=" +
                (resolvedKey == null ? 0 : resolvedKey.length()));
    }
}


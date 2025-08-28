package ict.project.feedback.core;

public interface LlmClient {
    LlmRewrite rewrite(String systemPrompt, String userPrompt);
}


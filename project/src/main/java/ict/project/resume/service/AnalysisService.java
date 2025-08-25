package ict.project.resume.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 서비스
 * - RAG 프롬프트 구성(RagService) + LLM 호출(LlmClientService) 결합
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RagService ragService;
    private final LlmClientService llmClientService;

    /** 최종 프롬프트 생성 + 모델 호출 */
    @Transactional(readOnly = true)
    public Result analyze(Integer userId) {
        String prompt = ragService.buildFeedbackPrompt(userId);
        String result = llmClientService.generateWithRag(prompt);
        return new Result(prompt, result);
    }

    /** 결과 DTO(내부) */
    public record Result(String prompt, String result) {}
}

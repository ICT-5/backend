package ict.project.resume.controller;

import ict.project.resume.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 분석 전용 컨트롤러
 * - RAG로 구성한 프롬프트를 기반으로 LLM 분석을 수행하는 엔드포인트
 * - 실제 LLM 연동은 runLlm(...) 내부 TODO 지점에서 교체
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final RagService ragService;

    /**
     * 분석 버튼 클릭 시 호출
     * - body: { "userId": 1, "includePosting": true }
     * - 동작: resume_feedback 프롬프트 + (전역 CORPUS) + 사용자 RAG(RESUME [+POSTING])로 최종 프롬프트 구성
     */
    @PostMapping(value = "/feedback", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest req) {
        try {
            boolean includePosting = req.includePosting() != null ? req.includePosting() : false; // 기본 true ->채용공고 rag에 넣기임 !!
            String prompt = ragService.buildFeedbackPrompt(req.userId(), includePosting);
            String result = runLlm(prompt); // TODO: 실제 LLM 연동으로 교체
            return ResponseEntity.ok(new AnalyzeResponse(prompt, result));
        } catch (Exception e) {
            log.error("분석 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 프롬프트 미리보기: 최종 프롬프트만 확인
     * - GET /api/analysis/prompt?userId=1&includePosting=false
     */
    @GetMapping("/prompt")
    public ResponseEntity<?> previewPrompt(@RequestParam Integer userId,
                                           @RequestParam(defaultValue = "false") boolean includePosting) {//채용공고 안들어감
        try {
            String prompt = ragService.buildFeedbackPrompt(userId, includePosting);
            return ResponseEntity.ok(new PromptPreview(prompt));
        } catch (Exception e) {
            log.error("프롬프트 미리보기 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 실제 LLM 연동 지점 (데모용) */
    private String runLlm(String prompt) {
        return """
               [DEMO_RESULT]
               아래 프롬프트를 기반으로 LLM을 연결해 결과를 생성하세요.
               - 프롬프트 토큰 길이/안전성 고려
               - 출력 포맷(강점/개선점/키워드) 합의 후 템플릿 적용
               """;
    }

    /** 요청 DTO */
    public record AnalyzeRequest(Integer userId, Boolean includePosting) {}

    /** 응답 DTO (프롬프트+결과) */
    public record AnalyzeResponse(String prompt, String result) {}

    /** 프롬프트 미리보기 DTO */
    public record PromptPreview(String prompt) {}
}

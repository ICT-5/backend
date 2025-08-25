package ict.project.resume.controller;

import ict.project.resume.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis/v2")
public class AnalysisControllerV2 {

    private final AnalysisService analysisService;

    @PostMapping(value = "/feedback", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest req) {
        try {
            AnalysisService.Result r = analysisService.analyze(req.userId());
            return ResponseEntity.ok(new AnalyzeResponse(r.prompt(), r.result()));
        } catch (Exception e) {
            log.error("분석 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/prompt")
    public ResponseEntity<?> preview(@RequestParam Integer userId) {
        try {
            AnalysisService.Result r = analysisService.analyze(userId);
            // 프롬프트만 보고 싶다면 r.result()는 무시 가능
            return ResponseEntity.ok(new PromptPreview(r.prompt()));
        } catch (Exception e) {
            log.error("프롬프트 미리보기 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record AnalyzeRequest(Integer userId) {}
    public record AnalyzeResponse(String prompt, String result) {}
    public record PromptPreview(String prompt) {}
}

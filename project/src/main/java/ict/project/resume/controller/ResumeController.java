package ict.project.resume.controller;

import ict.project.resume.entity.ResumeEntity;
import ict.project.resume.service.RagService;
import ict.project.resume.service.ResumeService;
import ict.project.resume.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ResumeController {

    private final ResumeService resumeService;
    private final RagService ragService;
    private final TextExtractionService textExtractionService;

    /** 자소서 업로드 + 채용공고 크롤링 */
    @PostMapping(value = "/resume/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResumeAndCrawl(
            @RequestParam Integer userId,
            @RequestPart("resumeFile") MultipartFile resumeFile,
            @RequestParam("jobUrl") String jobUrl
    ) {
        try {
            ResumeEntity savedResume = resumeService.saveResume(userId, resumeFile, jobUrl);
            ragService.ensureResumeFeedbackPrompt();

            String resumeContent = textExtractionService.extract(Path.of(savedResume.getFilePath()));
            ragService.registerUserChunks(userId, "RESUME", savedResume.getFilePath(), resumeContent);

            ResumeEntity updatedResume = resumeService.crawlJobPostingAndAttach(userId, jobUrl);
            String postingContent = textExtractionService.extract(Path.of(updatedResume.getJobfilePath()));
            ragService.registerResumeChunks(userId, updatedResume, postingContent, "POSTING");

            return ResponseEntity.ok(updatedResume);
        } catch (Exception e) {
            log.error("업로드 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 분석 프롬프트 생성 */
    @GetMapping("/resume/analyze")
    public ResponseEntity<?> analyzeResume(@RequestParam Integer userId) {
        try {
            resumeService.getLatestResume(userId);
            String prompt = ragService.buildFeedbackPrompt(userId, true);
            return ResponseEntity.ok(new AnalysisResponseDto(prompt, "LLM_RESULT_TODO"));
        } catch (Exception e) {
            log.error("분석 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record AnalysisResponseDto(String prompt, String result) {}
}

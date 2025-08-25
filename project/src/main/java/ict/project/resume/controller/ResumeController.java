package ict.project.resume.controller;

import ict.project.resume.entity.ResumeEntity;
import ict.project.resume.service.RagService;
import ict.project.resume.service.ResumeService;
import ict.project.resume.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
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


    /**
     * 자소서 업로드 + 채용공고 크롤링을 한 번에 처리
     * - 요청: multipart/form-data
     *   - userId: 1
     *   - resumeFile: (파일)
     *   - jobUrl: https://...
     * - 반환: 채용공고까지 붙은 updatedResume
     */
    @PostMapping(value = "/resume/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResumeAndCrawl(
            @RequestParam Integer userId,
            @RequestPart("resumeFile") MultipartFile resumeFile,
            @RequestParam("jobUrl") String jobUrl
    ) {
        try {
            // 1) 이력서 저장
            ResumeEntity savedResume = resumeService.saveResume(userId, resumeFile);
            ragService.ensureResumeFeedbackPrompt();

            // 2) 이력서 텍스트 추출 → RAG 저장(RESUME)
            String resumeContent = textExtractionService.extract(Path.of(savedResume.getFilePath()));
            if (resumeContent == null || resumeContent.strip().length() < 10) {
                return ResponseEntity.badRequest().body("업로드한 파일에서 텍스트를 추출하지 못했습니다. 다른 형식 또는 내용이 포함된 파일을 올려주세요.");
            }
            ragService.registerUserChunks(userId, "RESUME", savedResume.getFilePath(), resumeContent);

            // 3) 채용공고 크롤링 & 파일 저장 → 최신 ResumeEntity 업데이트
            ResumeEntity updatedResume = resumeService.crawlJobPostingAndAttach(userId, jobUrl);

            // 4) 채용공고 텍스트 추출 → RAG 저장(POSTING)
            String postingContent = textExtractionService.extract(Path.of(updatedResume.getJobfilePath()));
            if (postingContent == null || postingContent.strip().length() < 10) {
                return ResponseEntity.badRequest().body("채용공고에서 텍스트를 추출하지 못했습니다.");
            }
            ragService.registerResumeChunks(userId, updatedResume, postingContent, "POSTING");

            // ✅ 채용공고까지 붙여진 updatedResume 반환
            return ResponseEntity.ok(updatedResume);

        } catch (Exception e) {
            log.error("이력서+채용공고 업로드 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
//    @PostMapping(value = "/resume/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> uploadResume(@RequestParam Integer userId,
//                                          @RequestPart("file") MultipartFile file) {
//        try {
//            ResumeEntity saved = resumeService.saveResume(userId, file);
//            ragService.ensureResumeFeedbackPrompt();
//
//            // ⬇️ 여기! 바이너리도 안전하게 텍스트 추출
//            String content = textExtractionService.extract(Path.of(saved.getFilePath()));
//            if (content == null || content.strip().length() < 10) {
//                return ResponseEntity.badRequest().body("업로드한 파일에서 텍스트를 추출하지 못했습니다. 다른 형식 또는 내용이 포함된 파일을 올려주세요.");
//            }
//            ragService.registerUserChunks(userId, "RESUME", saved.getFilePath(), content);
//            return ResponseEntity.ok(saved);
//        } catch (Exception e) {
//            log.error("이력서 업로드 실패", e);
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @PostMapping(value = "/jobposting/crawl", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
//    public ResponseEntity<?> crawlJobPosting(@RequestBody JobCrawlRequestDto req) {
//        try {
//            var updated = resumeService.crawlJobPostingAndAttach(req.userId(), req.url());
//            ragService.ensureResumeFeedbackPrompt();
//
//            // 채용공고 저장 파일에서도 Tika로 추출 (HTML/텍스트 모두 커버)
//            String content = textExtractionService.extract(Path.of(updated.getJobfilePath()));
//            if (content == null || content.strip().length() < 10) {
//                return ResponseEntity.badRequest().body("채용공고에서 텍스트를 추출하지 못했습니다.");
//            }
//            ragService.registerResumeChunks(req.userId(), updated, content, "POSTING");
//            return ResponseEntity.ok(updated);
//        } catch (Exception e) {
//            log.error("채용공고 크롤링/저장 실패", e);
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

    @GetMapping("/resume/analyze")
    public ResponseEntity<?> analyzeResume(@RequestParam Integer userId) {
        try {
            resumeService.getLatestResume(userId);
            String prompt = ragService.buildFeedbackPrompt(userId, true);
            return ResponseEntity.ok(new AnalysisResponseDto(prompt, "LLM_RESULT_TODO"));
        } catch (Exception e) {
            log.error("이력서 분석 프롬프트 생성 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record JobCrawlRequestDto(Integer userId, String url) {}
    public record AnalysisResponseDto(String prompt, String result) {}
}


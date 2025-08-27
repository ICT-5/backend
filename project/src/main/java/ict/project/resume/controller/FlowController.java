package ict.project.resume.controller;

import ict.project.resume.entity.ResumeEntity;
import ict.project.resume.service.*;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flow")
@Validated
@RequiredArgsConstructor
public class FlowController {

    private final FileTextExtractor fileTextExtractor;
    private final JobPostingFetcher jobPostingFetcher;
    private final EmbeddingService embeddingService;
    private final ChromaVectorStoreService chromaVectorStoreService;
    private final LlmClientService llmClient;
    private final ResumeService resumeService;

//    public FlowController(
//            FileTextExtractor fileTextExtractor,
//            JobPostingFetcher jobPostingFetcher,
//            EmbeddingService embeddingService,
//            ChromaVectorStoreService chromaVectorStoreService,
//            LlmClientService llmClient // ⬅️ 추가
//    ) {
//        this.fileTextExtractor = fileTextExtractor;
//        this.jobPostingFetcher = jobPostingFetcher;
//        this.embeddingService = embeddingService;
//        this.chromaVectorStoreService = chromaVectorStoreService;
//        this.llmClient = llmClient; // ⬅️ 추가
//    }

    /**
     * multipart/form-data 업로드로 분석 실행
     */
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> analyzeMultipart(
            @RequestParam Integer userId,
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam String jobUrl,
            @RequestParam(defaultValue = "accepted-essays") String collection,
            @RequestParam(defaultValue = "5") @Min(1) Integer topK
    ) throws IOException {

        // 1) 파일 텍스트 추출
        String resumeText = fileTextExtractor.extract(resumeFile);

        // 2) 채용공고 텍스트
        String postingText = jobPostingFetcher.fetch(jobUrl);
        ResumeEntity savedResume = resumeService.saveResume(userId, resumeFile, jobUrl);

        // 3) 벡터 검색
        String resumeForEmbedding = limitForEmbedding(resumeText);
        List<Float> queryEmb = embeddingService.embedAsList(resumeForEmbedding);
        var hits = chromaVectorStoreService.search(collection, queryEmb, topK);

        // 3-1) 프롬프트용 코퍼스 블록 (id/distance/text 요약)
        String corpusBlock = hits.stream()
                .map(h -> "- (" + String.format(Locale.US, "%.4f", h.distance()) + ") "
                        + preview(nvl(h.text()), 600))
                .collect(Collectors.joining("\n"));

        // 3-2) 분석 생성 (LLM 호출)
        String prompt = buildRagPrompt(resumeText, postingText, corpusBlock);
        String analysis;
        try {
            // ⚠️ LlmClientService 메소드명은 프로젝트에 맞게 쓰세요.
            // 예: complete(prompt) / chat(prompt) / ask(prompt) 등
            analysis = nvl(llmClient.complete(prompt));
        } catch (Exception e) {
            analysis = "(분석 생성 실패: " + e.getMessage() + ")";
        }

        // 4) 응답 구성
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("userId", userId);
        resp.put("collection", collection);
        resp.put("topK", topK);
        resp.put("resumePreview", preview(resumeText, 200));
        resp.put("postingPreview", preview(postingText, 200));
        resp.put("analysis", analysis); // ⬅️ index.html 이 이 값을 그려줌
        resp.put("retrieved", hits.stream().map(h -> Map.of(
                "id", nvl(h.id()),
                "distance", h.distance(),
                "textPreview", preview(nvl(h.text()), 240)
        )).toList());

        return ResponseEntity.ok(resp);
    }

    // --------------------
    // helpers
    // --------------------

    /** 임베딩 모델 입력 길이 보호용(토큰이 아닌 문자 기준 대략 컷) */
    private static String limitForEmbedding(String s) {
        if (s == null) return "";
        int maxChars = 16_000;
        return s.length() > maxChars ? s.substring(0, maxChars) : s;
    }

    /** 프리뷰용 문자열 정리 + 말줄임 */
    private static String preview(String s, int maxLen) {
        if (s == null) return "";
        String compact = s.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLen) return compact;
        return compact.substring(0, Math.max(0, maxLen)) + "…";
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    @SuppressWarnings("unused")
    private static String fmt4(double v) { return String.format(Locale.US, "%.4f", v); }

    // --- 요청하신 RAG 프롬프트/트리머 ---
    private static String trimTo(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + " ...";
    }

    @SuppressWarnings("unused")
    private static String buildRagPrompt(String resumeText, String postingText, String corpusBlock) {
        return """
                당신은 채용 담당자입니다. 아래 자료를 바탕으로 지원자의 강점과 공고 요구사항의 적합도를 평가하고, 구체적 개선 제안을 제시하세요.

                [지원자 자소서]
                %s

                [채용 공고]
                %s

                [관련 참고 문서]
                %s

                출력 형식:
                1) 요약 평가 (3~5줄)
                2) 강점
                3) 보완점
                4) 구체적 개선 제안 (불릿)
                """.formatted(trimTo(resumeText, 3000), trimTo(postingText, 3000), trimTo(corpusBlock, 4000));
    }
}



//package ict.project.resume.controller;
//
//import ict.project.resume.service.ChromaVectorStoreService;
//import ict.project.resume.service.EmbeddingService;
//import ict.project.resume.service.FileTextExtractor;
//import ict.project.resume.service.JobPostingFetcher;
//import jakarta.validation.constraints.Min;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/flow")
//@Validated
//public class FlowController {
//
//    private final FileTextExtractor fileTextExtractor;
//    private final JobPostingFetcher jobPostingFetcher;
//    private final EmbeddingService embeddingService;
//    private final ChromaVectorStoreService chromaVectorStoreService;
//
//    public FlowController(
//            FileTextExtractor fileTextExtractor,
//            JobPostingFetcher jobPostingFetcher,
//            EmbeddingService embeddingService,
//            ChromaVectorStoreService chromaVectorStoreService
//    ) {
//        this.fileTextExtractor = fileTextExtractor;
//        this.jobPostingFetcher = jobPostingFetcher;
//        this.embeddingService = embeddingService;
//        this.chromaVectorStoreService = chromaVectorStoreService;
//    }
//
//    @PostMapping(
//            value = "/analyze",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE
//    )
//    public ResponseEntity<?> analyzeMultipart(
//            @RequestParam Long userId,
//            @RequestParam("resumeFile") MultipartFile resumeFile,
//            @RequestParam String jobUrl,
//            @RequestParam(defaultValue = "accepted-essays") String collection,
//            @RequestParam(defaultValue = "5") @Min(1) Integer topK
//    ) throws IOException {
//
//        // 1) 파일 텍스트 추출
//        String resumeText = fileTextExtractor.extract(resumeFile);
//
//        // 2) 채용공고 스크랩
//        String postingText = jobPostingFetcher.fetch(jobUrl);
//
//        // 3) 검색 실행 (임베딩 입력 길이 제한)
//        String resumeForEmbedding = limitForEmbedding(resumeText);
//        List<Float> queryEmb = embeddingService.embedAsList(resumeForEmbedding);
//        var hits = chromaVectorStoreService.search(collection, queryEmb, topK);
//
//        // 4) 응답
//        Map<String, Object> resp = new LinkedHashMap<>();
//        resp.put("ok", true);
//        resp.put("userId", userId);
//        resp.put("collection", collection);
//        resp.put("topK", topK);
//        resp.put("resumePreview", preview(resumeText, 200));
//        resp.put("postingPreview", preview(postingText, 200));
//        resp.put("retrieved", hits.stream().map(h -> Map.of(
//                "id", nvl(h.id()),
//                "distance", h.distance(),
//                "textPreview", preview(nvl(h.text()), 240)
//        )).toList());
//
//        return ResponseEntity.ok(resp);
//    }
//
//    // --------------------
//    // helpers
//    // --------------------
//
//    /** 임베딩 모델 입력 길이 보호용(문자 기준 대략 컷) */
//    private static String limitForEmbedding(String s) {
//        if (s == null) return "";
//        int maxChars = 16_000;
//        return s.length() > maxChars ? s.substring(0, maxChars) : s;
//    }
//
//    /** 프리뷰용 문자열 정리 + 말줄임 (유일한 preview) */
//    private static String preview(String s, int maxLen) {
//        if (s == null) return "";
//        String compact = s.replaceAll("\\s+", " ").trim();
//        if (compact.length() <= maxLen) return compact;
//        return compact.substring(0, Math.max(0, maxLen)) + "…";
//    }
//
//    private static String nvl(String s) {
//        return s == null ? "" : s;
//    }
//
//    @SuppressWarnings("unused")
//    private static String fmt4(double v) {
//        return String.format(Locale.US, "%.4f", v);
//    }
//
//    // ---------------------------------------------------
//    // 요청하셨던 보조 유틸: 이름 충돌 방지 위해 previewLegacy로 변경
//    // 필요시 이 메서드를 호출해서 사용하세요.
//    // ---------------------------------------------------
//    private static String trimTo(String text, int max) {
//        if (text == null) return "";
//        return text.length() <= max ? text : text.substring(0, max) + " ...";
//    }
//
//    // 기존에 주셨던 preview 시그니처와 같아 충돌하므로 이름 변경
//    @SuppressWarnings("unused")
//    private static String previewLegacy(String text, int max) {
//        return trimTo(text, max);
//    }
//
//    @SuppressWarnings("unused")
//    private static String buildRagPrompt(String resumeText, String postingText, String corpusBlock) {
//        return """
//                당신은 채용 담당자입니다. 아래 자료를 바탕으로 지원자의 강점과 공고 요구사항의 적합도를 평가하고, 구체적 개선 제안을 제시하세요.
//
//                [지원자 자소서]
//                %s
//
//                [채용 공고]
//                %s
//
//                [관련 참고 문서]
//                %s
//
//                출력 형식:
//                1) 요약 평가 (3~5줄)
//                2) 강점
//                3) 보완점
//                4) 구체적 개선 제안 (불릿)
//                """.formatted(trimTo(resumeText, 3000), trimTo(postingText, 3000), trimTo(corpusBlock, 4000));
//    }
//}


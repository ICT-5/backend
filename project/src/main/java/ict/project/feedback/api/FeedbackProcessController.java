package ict.project.feedback.api;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ict.project.feedback.api.dto.Annotation;
import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;
import ict.project.feedback.core.RewriteService;
import ict.project.feedback.core.RuleDetector;
import ict.project.feedback.infra.PdfExporter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackProcessController {

    private final RuleDetector ruleDetector;
    private final RewriteService rewriteService;
    private final PdfExporter pdfExporter; // PDF 생성기

    /**
     * 원샷 처리: 규칙탐지 + LLM 첨삭
     * POST /api/feedback/process
     */
    @PostMapping("/process")
    public FeedbackResponse process(@RequestBody ProcessReq req) {
        // 1) annotate 단계와 동일하게 FeedbackResponse 구성
        FeedbackResponse base = new FeedbackResponse();
        base.setApiVersion("1.0");
        base.setSessionId(req.getSessionId());
        base.setChecklist(List.of("전후 수치 1개 포함", "STAR 3문장 유지", "JD 키워드 1개 명시"));

        List<FeedbackItem> items = new ArrayList<>();
        for (QA qa : req.getQas()) {
            FeedbackItem it = new FeedbackItem();
            it.setQid(qa.getQid());
            it.setAnswer(qa.getAnswer());

            // 규칙탐지
            var hits = ruleDetector.detect(
                    qa.getAnswer(),
                    req.getJdKeywords() == null ? List.of() : req.getJdKeywords()
            );

            // annotations 생성
            List<Annotation> anns = new ArrayList<>();
            for (var h : hits) {
                Annotation a = new Annotation();
                Annotation.Span span = new Annotation.Span();
                span.setStart(h.start());
                span.setEnd(h.end());
                span.setText(h.text());
                a.setSpan(span);
                a.setCategory(h.category());

                switch (h.category()) {
                    case "no_metric" -> {
                        a.setComment("전후 수치를 넣어주세요");
                        a.setSuggest("예) LCP 4.3s→2.6s");
                    }
                    case "vague" -> {
                        a.setComment("방법을 더 구체적으로 써주세요");
                        a.setSuggest("예) lazy-loading, code splitting 등");
                    }
                    case "no_jd_match" -> {
                        a.setComment("JD 키워드 1개를 자연스럽게 삽입하세요");
                        a.setSuggest("예) Spring Boot, Kafka…");
                    }
                    default -> a.setComment("개선 필요");
                }
                anns.add(a);
            }
            it.setAnnotations(anns);
            items.add(it);
        }
        base.setItems(items);

        // 2) rewrite 단계 호출
        List<String> jd = req.getJdKeywords() == null ? List.of() : req.getJdKeywords();
        return rewriteService.rewrite(base, jd); // 최종 결과 반환(각 item.rewrite, jdInsert 세팅됨)
    }

    /**
     * PDF 내보내기
     * POST /api/feedback/export.pdf
     * Body: FeedbackResponse (process 결과 그대로)
     */
    @PostMapping(value = "/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@RequestBody FeedbackResponse feedback) {
        byte[] pdf = pdfExporter.render(feedback);
        String sid = (feedback.getSessionId() == null || feedback.getSessionId().isBlank())
                ? "session" : feedback.getSessionId();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"feedback-" + sid + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ====== 요청 DTO ======
    @Data
    public static class ProcessReq {
        private String sessionId;
        private List<String> jdKeywords;
        private List<QA> qas;
    }

    @Data
    public static class QA {
        private String qid;
        private String answer;
    }
}

// src/main/java/org/example/feedback/api/SimulationWebhookController.java
package ict.project.feedback.api;

import lombok.RequiredArgsConstructor;
import ict.project.feedback.api.dto.FeedbackResponse;
import ict.project.feedback.api.dto.SimulationPayload;
import ict.project.feedback.core.RewriteService;
import ict.project.feedback.core.RuleDetector;
import ict.project.feedback.core.SimulationAdapter;
import ict.project.feedback.infra.PdfExporter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sim")
public class SimulationWebhookController {

    private final SimulationAdapter adapter;
    private final RuleDetector ruleDetector;
    private final RewriteService rewriteService;
    private final PdfExporter pdfExporter;

    private static final String SHARED_SECRET = "change-me-secret"; // 환경변수로 빼도 됨

    @PostMapping(value="/feedback", consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    public FeedbackResponse feedback(@RequestBody SimulationPayload payload,
                                     @RequestHeader(value="X-Sim-Secret", required=false) String secret) {
        // (옵션) 간단 인증
        // if (!SHARED_SECRET.equals(secret)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid secret");

        var base = adapter.toFeedbackResponse(payload);
        List<String> jd = payload.getJdKeywords()==null ? List.of() : payload.getJdKeywords();

        var annotated = ruleDetector.annotate(base, jd);
        return rewriteService.rewrite(annotated, jd);
    }

    @PostMapping(value="/feedback.pdf", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> feedbackPdf(@RequestBody SimulationPayload payload,
                                              @RequestHeader(value="X-Sim-Secret", required=false) String secret) {
        // if (!SHARED_SECRET.equals(secret)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid secret");

        var json = feedback(payload, secret);  // 위 로직 재사용
        byte[] pdf = pdfExporter.render(json);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"feedback-"+json.getSessionId()+".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

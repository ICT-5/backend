package ict.project.feedback.api;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ict.project.feedback.api.dto.FeedbackResponse;
import ict.project.feedback.core.RewriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class RewriteController {

    private final RewriteService rewriteService;

    // A의 응답(FeedbackResponse) + (옵션) jdKeywords를 입력으로 받음
    @PostMapping("/rewrite")
    public ResponseEntity<FeedbackResponse> rewrite(@Valid @RequestBody RewriteRequest body) {
        System.out.println("[/rewrite] items=" + (body.getResponse()==null? "null" : body.getResponse().getItems().size())
                + ", jdKeywords=" + body.getJdKeywords());
        return ResponseEntity.ok(rewriteService.rewrite(body.getResponse(), body.getJdKeywords()));
    }


    @Data
    public static class RewriteRequest {
        @Valid
        private FeedbackResponse response;
        private List<String> jdKeywords; // 선택
    }
}


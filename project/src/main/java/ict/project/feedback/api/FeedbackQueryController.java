// src/main/java/ict/project/feedback/api/FeedbackQueryController.java
package ict.project.feedback.api;

import ict.project.feedback.core.QaProjection;
import ict.project.feedback.core.SimulationQaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackQueryController {

    private final SimulationQaRepository qaRepo;

    @GetMapping("/qas")
    public ResponseEntity<?> getQas(@RequestParam Long sessionId) {
        System.out.println("[FEEDBACK] /qas called with sessionId=" + sessionId);

        List<QaProjection> rows = qaRepo.findAllBySessionId(sessionId);
        System.out.println("[FEEDBACK] rows from DB=" + rows.size());

        for (QaProjection p : rows) {
            System.out.println(" -> qid=" + p.getQid() + ", answer=" + p.getAnswer());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (QaProjection p : rows) {
            String ans = Optional.ofNullable(p.getAnswer()).orElse("").trim();
            if (ans.isEmpty()) continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("qid", String.valueOf(p.getQid()));
            m.put("answer", ans);
            out.add(m);
        }

        System.out.println("[FEEDBACK] final response size=" + out.size());
        return ResponseEntity.ok(out);
    }
}

package ict.project.resume.controller;

import ict.project.resume.service.BackfillAcceptedEssaysService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chroma")
@RequiredArgsConstructor
public class AdminChromaBackfillController {

    private final BackfillAcceptedEssaysService backfillAcceptedEssaysService;

    @PostMapping("/backfill/accepted")
    public ResponseEntity<?> backfillAccepted(
            @RequestParam(defaultValue = "accepted-essays") String collection,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "0") int maxDocs
    ) {
        backfillAcceptedEssaysService.runBackfill(collection, pageSize, maxDocs);
        return ResponseEntity.ok(
                java.util.Map.of("ok", true, "collection", collection, "pageSize", pageSize, "maxDocs", maxDocs)
        );
    }
}

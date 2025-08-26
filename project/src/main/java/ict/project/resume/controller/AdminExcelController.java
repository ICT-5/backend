// src/main/java/ict/project/resume/controller/AdminExcelController.java
package ict.project.resume.controller;

import ict.project.resume.service.BulkExcelIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class AdminExcelController {

    private final BulkExcelIngestService bulkExcelIngestService;

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "accepted-essays") String collection, // 현재는 내부에서 "resumes" 컬렉션 사용; 필요 시 서비스에 전달하도록 확장 가능
            @RequestParam(defaultValue = "800") int maxLen,
            @RequestParam(defaultValue = "PASS_SAMPLE") String sourceTag
    ) {
        try {
            int ingested = bulkExcelIngestService.ingest(file.getInputStream(), sourceTag, maxLen);
            return ResponseEntity.ok(
                    Map.of("ok", true, "ingested", ingested, "collection", collection, "maxLen", maxLen, "sourceTag", sourceTag)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("엑셀 적재 실패: " + e.getMessage());
        }
    }
}

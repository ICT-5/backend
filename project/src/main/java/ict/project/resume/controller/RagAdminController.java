package ict.project.resume.controller;

import ict.project.resume.entity.RagChunkEntity;
import ict.project.resume.entity.RagSettingsEntity;
import ict.project.resume.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag/admin")
public class RagAdminController {

    private final RagService ragService;

    @Value("${app.upload.root:C:/ict-uploads}")
    private String uploadRoot;

    @Value("${app.rag.admin-user-id:1}")
    private Integer adminUserId;

    /** 전역 코퍼스 업로드 */
    @PostMapping(value = "/corpus/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCorpus(@RequestPart("file") MultipartFile file) {
        try {
            Path dir = Paths.get(uploadRoot, "corpus", String.valueOf(adminUserId));
            Files.createDirectories(dir);

            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String savedName = System.currentTimeMillis() + "_" + original;
            Path savedPath = dir.resolve(savedName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, savedPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String content = extractTextWithTika(savedPath);
            ragService.registerCorpusChunks(adminUserId, savedPath.toString(), content);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("코퍼스 업로드 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 전역 resume_feedback 프롬프트 보장/조회 */
    @PostMapping("/prompt/ensure")
    public ResponseEntity<?> ensurePrompt() {
        try {
            ragService.ensureResumeFeedbackPrompt();
            RagSettingsEntity setting = ragService.getResumeFeedbackSetting();
            return ResponseEntity.ok(new PromptResponse(
                    setting.getName(),
                    setting.getPromptText(),
                    setting.getUpdatedAt() == null ? null : setting.getUpdatedAt().toString()
            ));
        } catch (Exception e) {
            log.error("프롬프트 ensure 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 전역 resume_feedback 프롬프트 조회 */
    @GetMapping("/prompt")
    public ResponseEntity<?> getPrompt() {
        try {
            RagSettingsEntity setting = ragService.getResumeFeedbackSetting();
            return ResponseEntity.ok(new PromptResponse(
                    setting.getName(),
                    setting.getPromptText(),
                    setting.getUpdatedAt() == null ? null : setting.getUpdatedAt().toString()
            ));
        } catch (Exception e) {
            log.error("프롬프트 조회 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** ✅ 전역 코퍼스 청크 조회 (요청하신 경로) */
    @GetMapping("/corpus/chunks")
    public ResponseEntity<?> listCorpusChunks() {
        try {
            List<RagChunkEntity> chunks = ragService.getCorpusChunks(adminUserId);
            // 가벼운 DTO로 변환하여 반환
            return ResponseEntity.ok(
                    chunks.stream().map(c ->
                            new CorpusChunkDto(
                                    c.getRagId(),
                                    c.getSource(),
                                    c.getFilePath(),
                                    c.getContent(),     // 필요 시 앞부분만 잘라서 보내세요
                                    c.getCreatedAt() == null ? null : c.getCreatedAt().toString()
                            )
                    ).toList()
            );
        } catch (Exception e) {
            log.error("코퍼스 청크 조회 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 전역 코퍼스 전체 삭제 */
    @DeleteMapping("/corpus")
    public ResponseEntity<?> deleteAllCorpus() {
        try {
            int deleted = ragService.deleteCorpusChunks(adminUserId);
            return ResponseEntity.ok(new DeleteCorpusResponse(deleted));
        } catch (Exception e) {
            log.error("코퍼스 삭제 실패", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ===========================
     * 내부 유틸: Tika 텍스트 추출
     * =========================== */
    private String extractTextWithTika(Path filePath) {
        try {
            Tika tika = new Tika();
            return tika.parseToString(filePath.toFile());
        } catch (Exception e) {
            log.error("텍스트 추출 실패: {}", filePath, e);
            return "";
        }
    }

    /* DTOs */
    public record PromptResponse(String name, String promptText, String updatedAt) {}
    public record DeleteCorpusResponse(int deleted) {}
    public record CorpusChunkDto(Integer ragId, String source, String filePath, String content, String createdAt) {}
}

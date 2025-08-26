package ict.project.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 청크 조회/응답용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChunkDto {
    private Integer ragId;
    private Integer userId;
    private String source;
    private String filePath;
    private String content;
    private String embeddingJson;
    private LocalDateTime createdAt;
}

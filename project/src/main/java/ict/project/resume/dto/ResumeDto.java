package ict.project.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이력서 조회/응답용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private Integer resumeId;
    private Integer userId;
    private String fileName;
    private String filePath;
    private String jobfileName;
    private String jobfilePath;
    private LocalDateTime updatedAt;
}

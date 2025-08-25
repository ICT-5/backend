package ict.project.resume.entity;

import ict.project.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "resume",
        indexes = {
                @Index(name = "idx_resume_user", columnList = "id")
        }
)
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id")
    private Integer resumeId;

    // FK 대상: User.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_resume_user"))
    private UserEntity user;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 채용공고(크롤링 결과) 저장 파일명/경로
    @Column(name = "jobfile_name", length = 255)
    private String jobfileName;

    @Column(name = "jobfile_path", length = 255)
    private String jobfilePath;
}



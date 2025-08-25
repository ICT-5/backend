package ict.project.resume.entity;

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
        name = "rag_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rag_settings_name", columnNames = {"name"})
        }
)
public class RagSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 여러 행 구분용 PK

    @Column(name = "name", length = 255, nullable = false)
    private String name; // 설정 이름(고유 키)

    @Lob
    @Column(name = "prompt_text")
    private String promptText; // 프롬프트 내용

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

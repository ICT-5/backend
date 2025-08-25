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
        name = "rag_chunk",
        indexes = {
                @Index(name = "idx_rag_user", columnList = "user_id"),
                @Index(name = "idx_rag_source", columnList = "source")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_rag_unique",
                        columnNames = {"user_id", "source", "file_path", "content_hash"}
                )
        }
)
public class RagChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_id")
    private Integer ragId;

    // FK → User.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rag_user"))
    private UserEntity user;

    @Column(name = "source", length = 32, nullable = false)
    private String source; // 'CORPUS' / 'RESUME' / 'POSTING'

    @Column(name = "file_path", length = 255)
    private String filePath;

    // 본문이 클 수 있으므로 MEDIUMTEXT 매핑
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    // JSON 문자열로 저장 (임베딩 벡터 직렬화)
    @Column(name = "embedding_json", columnDefinition = "JSON", nullable = false)
    private String embeddingJson;

    // DB DEFAULT CURRENT_TIMESTAMP 사용 (insertable/updatable=false)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // content_hash는 MySQL VIRTUAL 컬럼이므로 매핑하지 않음
}

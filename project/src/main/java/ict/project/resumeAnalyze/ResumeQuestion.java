package ict.project.resumeAnalyze;

import ict.project.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "resume_question")
public class ResumeQuestion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id; // 질문 PK

    @Column(nullable = false, length = 1000)
    private String question; // 생성된 질문

    // User와 다대일 관계 (여러 질문 → 한 유저)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK
    private UserEntity user;
}

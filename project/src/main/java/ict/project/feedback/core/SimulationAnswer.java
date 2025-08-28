// src/main/java/ict/project/feedback/core/SimulationAnswer.java
package ict.project.feedback.core;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "simulation_answer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimulationAnswer {
    @Id
    @Column(name = "answer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @Column(name = "sim_question_id", nullable = false)
    private Long simQuestionId;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}


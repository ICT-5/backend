// src/main/java/ict/project/feedback/core/SimulationQuestion.java
package ict.project.feedback.core;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "simulation_question")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimulationQuestion {
    @Id
    @Column(name = "sim_question_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long simQuestionId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "asked_at", nullable = false)
    private LocalDateTime askedAt;
}


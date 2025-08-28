package ict.project.feedback.core;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="interview_session")
@Getter @Setter
public class InterviewSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", unique = true, nullable = false, length = 100)
    private String sessionId;

    @Column(nullable = false)
    private Integer userId;

    @Column(length = 50)
    private String interviewType;

    @Column(length = 50)
    private String difficulty;

    @CreationTimestamp
    private LocalDateTime createdAt;
}


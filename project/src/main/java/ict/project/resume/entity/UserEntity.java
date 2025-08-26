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
        name = "User",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_provider_providerId", columnNames = {"provider", "providerId"})
        }
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 유저아이디 (PK)

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String provider;

    @Column(name = "providerId")
    private String providerId;

    @Column(name = "createDate")
    private LocalDateTime createDate;


    @Column(length = 255)
    private String job_title;

    @Lob
    private String education_career;

    @Lob
    private String tech_stack;

    // 컬럼명이 'Field' 이므로 명시적 매핑
    @Column(name = "Field", length = 255)
    private String fieldName;
}


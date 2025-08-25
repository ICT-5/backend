package ict.project.user;

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
    private String password;

    @Column(length = 255)
    private String provider;
    @Column(length = 255)
    private String providerId;

    // String → DATETIME 매핑
    private LocalDateTime createDate;

    @Lob
    private String techStack; //기술스택
    @Lob
    private String jobCareer; //경력
    @Lob
    private String educationCareer; //학력
    @Column(length = 255)
    private String jobCategory;  //희망 직군
    private String jobRole;      //희망 직무
}


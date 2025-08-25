package ict.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저 조회/응답용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer id;
    private String email;
    private String username;
    private String provider;
    private String providerId;
    private LocalDateTime createDate;
    private String jobTitle;
    private String educationCareer;
    private String techStack;
    private String fieldName;
}


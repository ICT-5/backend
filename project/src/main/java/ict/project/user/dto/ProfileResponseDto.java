package ict.project.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponseDto {
    private String username;
    private String email;
    private String techStack;
    private String jobCareer;
    private String educationCareer;
    private String jobCategory;
    private String jobRole;
}

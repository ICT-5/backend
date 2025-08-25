package ict.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequestDto {
    private String techStack;        // 기술스택 (예: "CSS, React, Spring Boot")
    private String jobCareer;        // 경력 (예: "3년 이상")
    private String educationCareer;  // 학력 (예: "학사")
    private String jobCategory;      // 희망 직군 (예: "IT 서비스")
    private String jobRole;          // 희망 직무 (예: "웹/SW 기획")
}

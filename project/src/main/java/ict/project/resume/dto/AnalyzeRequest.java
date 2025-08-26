// DTO
// src/main/java/ict/project/resume/dto/AnalyzeRequest.java
package ict.project.resume.dto;

import lombok.Data;

@Data
public class AnalyzeRequest {
    private String collection= "accepted-essays";
    private Integer topK= 5;
    private String resumeText;
    private String postingText;
    private String queryText; // 없으면 resumeText로 대체
}

//@Data
//public class AnalyzeRequest {
//    private String resumeText;            // 필수
//    private String postingText;           // 필수
//    private String collection = "accepted-essays"; // 옵션
//    private Integer topK = 5;                        // 옵션
//}

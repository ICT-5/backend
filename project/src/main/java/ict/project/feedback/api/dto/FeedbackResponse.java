package ict.project.feedback.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private String apiVersion;
    private String sessionId;
    private List<String> checklist;
    private List<FeedbackItem> items; // ← 중첩 Item 대신 별도 클래스 사용
}

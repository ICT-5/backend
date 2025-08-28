package ict.project.feedback.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class FeedbackItem {
    private String qid;
    private String question;     // ✅ 추가: 질문 텍스트
    private String answer;
    private List<Annotation> annotations;
    private String rewrite;
    private List<String> jdInsert;
}




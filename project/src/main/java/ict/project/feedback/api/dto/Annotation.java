package ict.project.feedback.api.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class Annotation {
    private Span span;
    private String category;
    private String comment;
    private String suggest;

    // 👇 내부 static 클래스로 Span 정의 추가
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Span {
        private int start;
        private int end;
        private String text;
    }
}



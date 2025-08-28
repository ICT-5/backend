// src/main/java/org/example/feedback/api/dto/SimulationPayload.java
package ict.project.feedback.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationPayload {
    private String sessionId;
    private List<Entry> entries;
    private List<String> jdKeywords;
    private List<String> checklist;
    private Map<String,Object> meta;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String simQuestionId;
        private String question;
        private String answer;   // 필수
        private Long askedAt;
    }
}


package ict.project.feedback.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record FeedbackRequest(
        @NotBlank String apiVersion,
        @NotBlank String sessionId,
        @NotBlank String jobRole,
        @NotNull Persona persona,
        @Size(min=1, max=12) List<@NotBlank String> jdKeywords,
        @Size(min=1, max=10) List<QA> qas
){
    public record Persona(@NotBlank String type, @NotBlank String difficulty) {}
    public record QA(@NotBlank String qid, @NotBlank String question, @NotBlank @Size(max=1500) String answer) {}
}


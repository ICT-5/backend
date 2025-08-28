package ict.project.feedback.api;

import lombok.RequiredArgsConstructor;
import ict.project.feedback.api.dto.Annotation;
import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackRequest;
import ict.project.feedback.api.dto.FeedbackResponse;
import ict.project.feedback.api.dto.Annotation;
import ict.project.feedback.core.RuleDetector;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final RuleDetector detector; // 규칙 탐지 엔진 주입

    @PostMapping("/annotate")
    public FeedbackResponse annotate(@RequestBody FeedbackRequest req) {

        // qas 전체를 돌면서 아이템 생성
        var items = req.qas().stream().map(qa -> {
            var hits = detector.detect(qa.answer(), req.jdKeywords());

            var annotations = hits.stream()
                    .map(h -> new Annotation(
                            new ict.project.feedback.api.dto.Annotation.Span(h.start(), h.end(), h.text()),
                            h.category(),
                            switch (h.category()) {
                                case "no_metric"   -> "전후 수치를 넣어주세요";
                                case "vague"       -> "방법을 더 구체적으로 써주세요";
                                case "no_jd_match" -> "직무 키워드를 1개 이상 포함하세요";
                                default            -> "보완이 필요합니다";
                            },
                            switch (h.category()) {
                                case "no_metric"   -> "예) LCP 4.3s→2.6s";
                                case "vague"       -> "예) lazy-loading, code splitting 등";
                                case "no_jd_match" -> "예) Spring Boot, Kafka 등 JD 삽입";
                                default            -> "예시를 추가해 주세요";
                            }
                    )).toList();

            // 중첩 타입 대신 별도 DTO 사용
            FeedbackItem item = new FeedbackItem();
            item.setQid(qa.qid());
            item.setAnswer(qa.answer());
            item.setAnnotations(annotations);
            item.setRewrite("");         // B가 채울 예정
            item.setJdInsert(List.of()); // B가 채울 예정
            return item;
        }).toList();

        return new FeedbackResponse(
                "1.0",
                req.sessionId(),
                List.of("전후 수치 1개 포함", "STAR 3문장 유지", "JD 키워드 1개 명시"),
                items
        );
    }
}

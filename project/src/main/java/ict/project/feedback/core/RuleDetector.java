package ict.project.feedback.core;

import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleDetector {

    public record SpanHit(int start, int end, String text, String category) {}

    private static final Pattern VAGUE  = Pattern.compile("했다|좋았다|같다|도움|개선");
    private static final Pattern METRIC = Pattern.compile("\\d+(?:\\.\\d+)?\\s*(%|ms|초|점|LCP|CLS|CTR|전환율)", Pattern.CASE_INSENSITIVE);

    public FeedbackResponse annotate(FeedbackResponse response, List<String> jdKeywords) {
        if (response == null || response.getItems() == null) return response;

        for (FeedbackItem item : response.getItems()) {
            String answer = nz(item.getAnswer());
            List<SpanHit> hits = detect(answer, jdKeywords);

            List<ict.project.feedback.api.dto.Annotation> anns = new ArrayList<>();
            for (SpanHit h : hits) {
                anns.add(toAnnotation(h));
            }
            item.setAnnotations(anns);
        }
        return response;
    }

    public List<SpanHit> detect(String answer, List<String> jdKeywords) {
        List<SpanHit> hits = new ArrayList<>();
        if (answer == null || answer.isBlank()) return hits;

        Matcher mv = VAGUE.matcher(answer);
        if (mv.find()) {
            hits.add(new SpanHit(mv.start(), mv.end(), answer.substring(mv.start(), mv.end()), "vague"));
        }

        boolean hasMetric = METRIC.matcher(answer).find();
        if (!hasMetric) {
            int end = Math.min(Math.max(16, answer.length()/3), answer.length());
            hits.add(new SpanHit(0, end, answer.substring(0, end), "no_metric"));
        }

        boolean jdHit = jdKeywords != null && jdKeywords.stream()
                .anyMatch(k -> answer.toLowerCase().contains(nz(k).toLowerCase()));
        if (!jdHit) {
            int end = Math.min(12, answer.length());
            hits.add(new SpanHit(0, end, answer.substring(0, end), "no_jd_match"));
        }

        return mergeOverlaps(hits, answer);
    }

    private List<SpanHit> mergeOverlaps(List<SpanHit> list, String answer) {
        if (list.isEmpty()) return list;
        list.sort(Comparator.comparingInt(SpanHit::start));

        List<SpanHit> out = new ArrayList<>();
        SpanHit cur = list.get(0);

        for (int i = 1; i < list.size(); i++) {
            SpanHit nxt = list.get(i);
            boolean overlap = nxt.start() <= cur.end();
            boolean sameCat = nxt.category().equals(cur.category());

            if (overlap && sameCat) {
                int s = cur.start();
                int e = Math.max(cur.end(), nxt.end());
                cur = new SpanHit(s, e, slice(answer, s, e), cur.category());
            } else {
                out.add(cur);
                cur = nxt;
            }
        }
        out.add(cur);
        return out;
    }

    private ict.project.feedback.api.dto.Annotation toAnnotation(SpanHit h) {
        var ann = new ict.project.feedback.api.dto.Annotation();
        ann.setCategory(h.category());

        switch (h.category()) {
            case "no_metric" -> {
                ann.setComment("전후 수치를 넣어주세요");
                ann.setSuggest("예) LCP 4.3초→2.6초, 오류율 1.2%→0.6%");
            }
            case "vague" -> {
                ann.setComment("방법을 더 구체적으로 써주세요");
                ann.setSuggest("예) lazy-loading, code splitting, 캐시 전략 등");
            }
            case "no_jd_match" -> {
                ann.setComment("JD 키워드를 1개 이상 자연스럽게 포함하세요");
                ann.setSuggest("예) Spring Boot, Kafka, REST API, Docker 중 1개 이상");
            }
            default -> ann.setComment("개선이 필요합니다");
        }

        // ★ 여기서도 완전수식으로 Span 생성 (내부클래스 존재 가정)
        var span = new ict.project.feedback.api.dto.Annotation.Span();
        span.setStart(h.start());
        span.setEnd(h.end());
        span.setText(nz(h.text()));
        ann.setSpan(span);

        return ann;
    }

    private static String nz(String s){ return s == null ? "" : s; }
    private static String slice(String s, int start, int end) {
        int a = Math.max(0, start);
        int b = Math.min(s.length(), end);
        return a <= b ? s.substring(a, b) : "";
    }
}

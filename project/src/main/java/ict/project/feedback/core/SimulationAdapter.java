package ict.project.feedback.core;

import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;
import ict.project.feedback.api.dto.SimulationPayload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimulationAdapter {

    public FeedbackResponse toFeedbackResponse(SimulationPayload p) {
        FeedbackResponse r = new FeedbackResponse();
        r.setApiVersion("1.0");
        r.setSessionId(p.getSessionId() == null ? "sim_unknown" : p.getSessionId());

        List<String> ck = (p.getChecklist()==null || p.getChecklist().isEmpty())
                ? List.of("전후 수치 1개 포함","STAR 3문장 유지","JD 키워드 1개 명시")
                : p.getChecklist();
        r.setChecklist(ck);

        AtomicInteger idx = new AtomicInteger(1);
        List<FeedbackItem> items = p.getEntries().stream().map(e -> {
            FeedbackItem it = new FeedbackItem();
            it.setQid(e.getSimQuestionId() != null ? e.getSimQuestionId() : "q"+idx.getAndIncrement());
            it.setQuestion(e.getQuestion());                  // ✅ 질문 채우기
            it.setAnswer(e.getAnswer()==null ? "" : e.getAnswer());
            it.setAnnotations(List.of());
            it.setRewrite("");
            it.setJdInsert(List.of());
            return it;
        }).toList();

        r.setItems(items);
        return r;
    }
}

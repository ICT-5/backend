package ict.project.feedback.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewriteService {

    // llm.mode=springai 일 때 SpringAiLlmClient만 빈으로 등록됨
    private final LlmClient llm;

    private static final int MAX_PARALLEL = 4;
    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL);

    @PostConstruct
    public void init() {
        System.out.println("[RewriteService] LLM bean = " +
                (llm == null ? "null" : llm.getClass().getName()));
    }

    public FeedbackResponse rewrite(FeedbackResponse annotated, List<String> jdKeywords) {
        List<FeedbackItem> items = Optional.ofNullable(annotated.getItems()).orElse(List.of());
        System.out.println("[RewriteService] incoming items=" + items.size()
                + ", jdKeywords=" + (jdKeywords == null ? "null" : jdKeywords));

        List<CompletableFuture<FeedbackItem>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(
                        () -> rewriteOne(annotated, item, jdKeywords == null ? List.of() : jdKeywords),
                        pool))
                .collect(Collectors.toList());

        List<FeedbackItem> rewritten = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        annotated.setItems(rewritten);
        return annotated;
    }

    private FeedbackItem rewriteOne(FeedbackResponse ctx, FeedbackItem item, List<String> jdKeywords) {
        boolean needs = item.getAnnotations() != null && !item.getAnnotations().isEmpty();
        System.out.println("[rewriteOne] qid=" + item.getQid()
                + ", needs=" + needs
                + ", ann.size=" + (item.getAnnotations() == null ? 0 : item.getAnnotations().size()));

        if (!needs) {
            // 규칙 탐지가 없으면 LLM 호출 스킵: 원문을 그대로 반환(안전 기본값)
            item.setRewrite(item.getAnswer());
            item.setJdInsert(Collections.emptyList());
            return item;
        }

        // 1차: 표준 프롬프트
        String sys = PromptFactory.system();
        String usr = PromptFactory.user(ctx, item, jdKeywords);
        System.out.println("[rewriteOne] prompt sizes: sys=" + len(sys) + ", user=" + len(usr));

        try {
            System.out.println("[rewriteOne] calling LLM (pass1)...");
            LlmRewrite out = llm.rewrite(sys, usr);

            String pass1Rewrite = safe(out == null ? null : out.rewrite());
            List<String> pass1Jd = out == null || out.jdInsert() == null ? List.of() : out.jdInsert();

            // no-op이면 2차: 더 강한 프롬프트로 재시도
            if (isNoop(item.getAnswer(), pass1Rewrite)) {
                System.out.println("[rewriteOne] no-op detected → calling LLM (pass2, strict)...");
                String sys2 = PromptFactory.systemStrict();
                String usr2 = PromptFactory.userStrict(ctx, item, jdKeywords);
                LlmRewrite out2 = llm.rewrite(sys2, usr2);

                String pass2Rewrite = safe(out2 == null ? null : out2.rewrite());
                List<String> pass2Jd = out2 == null || out2.jdInsert() == null ? List.of() : out2.jdInsert();

                // 2차도 비었으면 최종 폴백: 원문
                String finalRewrite = pass2Rewrite.isBlank() ? item.getAnswer() : pass2Rewrite;
                List<String> finalJd = !pass2Jd.isEmpty() ? pass2Jd : pass1Jd;

                item.setRewrite(finalRewrite);
                item.setJdInsert(finalJd);
            } else {
                // 1차 결과가 유효
                String finalRewrite = pass1Rewrite.isBlank() ? item.getAnswer() : pass1Rewrite;
                item.setRewrite(finalRewrite);
                item.setJdInsert(pass1Jd);
            }
        } catch (Exception e) {
            System.err.println("[rewriteOne] LLM call failed for qid=" + item.getQid());
            e.printStackTrace();
            item.setRewrite(item.getAnswer());
            item.setJdInsert(Collections.emptyList());
        }
        return item;
    }

    // ===== 유틸 =====
    private static boolean isNoop(String original, String rewritten) {
        if (rewritten == null || rewritten.isBlank()) return true;
        String a = safe(original).replaceAll("\\s+","").toLowerCase();
        String b = safe(rewritten).replaceAll("\\s+","").toLowerCase();
        return a.equals(b);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static int len(String s) { return s == null ? 0 : s.length(); }
}

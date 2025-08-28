// src/main/java/org/example/feedback/infra/PdfExporter.java
package ict.project.feedback.infra;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import ict.project.feedback.api.dto.Annotation;
import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PdfExporter {

    // 리소스 경로: src/main/resources/fonts/NotoSansKR-Regular.ttf
    private static final String FONT_PATH = "fonts/NotoSansKR-Regular.ttf";

    public byte[] render(FeedbackResponse feedback) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===== Font (실패 시 예외. 폴백 없음) =====
            BaseFont bf = loadBaseFontFromClasspath(FONT_PATH);
            System.out.println("[PDF] Loaded font: " + FONT_PATH);
            Font fTitle = new Font(bf, 18, Font.BOLD);
            Font fH2    = new Font(bf, 14, Font.BOLD);
            Font fBold  = new Font(bf, 12, Font.BOLD);
            Font fBody  = new Font(bf, 12);
            Font fMono  = new Font(bf, 11);
            Font fGrey  = new Font(bf, 11, Font.ITALIC, new Color(110,110,110));

            // ===== Header =====
            String sessionId = nz(feedback == null ? null : feedback.getSessionId());
            String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            doc.add(new Paragraph("면접 답변 피드백 리포트", fTitle));
            doc.add(new Paragraph("세션 ID: " + sessionId + "   생성 시각: " + createdAt, fGrey));
            doc.add(Spacer(10));

            // ===== Checklist =====
            doc.add(new Paragraph("이번 답변에서 챙길 포인트", fH2));
            for (String c : nlist(feedback == null ? null : feedback.getChecklist())) {
                doc.add(bullet("• " + nz(c), fBody));
            }
            doc.add(Spacer(8));

            // ===== Items =====
            int idx = 1;
            for (FeedbackItem item : nlist(feedback == null ? null : feedback.getItems())) {
                String qid = nz(item.getQid());
                String q   = nz(item.getQuestion());
                String ans = nz(item.getAnswer());
                String rew = nz(item.getRewrite());
                List<Annotation> anns = nlist(item.getAnnotations());
                List<String> jd = nlist(item.getJdInsert());

                doc.add(divider());
                doc.add(new Paragraph("문항 #" + (idx++) + (qid.isBlank() ? "" : "  (QID: " + qid + ")"), fH2));
                if (!q.isBlank()) {
                    doc.add(new Paragraph("질문", fBold));
                    doc.add(codeBlock(q, fMono));
                    doc.add(Spacer(4));
                }

                // 현재 답변
                doc.add(new Paragraph("현재 답변", fBold));
                doc.add(codeBlock(ans, fMono));
                doc.add(Spacer(4));

                // 개선 필요
                doc.add(new Paragraph("개선이 필요한 부분", fBold));
                if (anns.isEmpty()) {
                    doc.add(new Paragraph("• 규칙 위반 없음 (핵심은 전달되지만 더 강하게 만들 여지는 있습니다.)", fBody));
                } else {
                    for (Annotation a : anns) {
                        String cat = nz(a.getCategory());
                        String spanTxt = a.getSpan()==null ? "" : nz(a.getSpan().getText());
                        String cmt = nz(a.getComment());
                        Paragraph p = new Paragraph("• [" + displayCat(cat) + "] " + cmt, fBody);
                        doc.add(p);
                        if (!spanTxt.isBlank()) {
                            doc.add(indent(new Paragraph("확인 구간: \"" + spanTxt + "\"", fMono)));
                        }
                    }
                }
                doc.add(Spacer(4));

                // 수정 가이드
                doc.add(new Paragraph("수정 가이드", fBold));
                if (anns.stream().anyMatch(a -> "no_metric".equalsIgnoreCase(nz(a.getCategory())))) {
                    doc.add(bullet("• 전후 수치를 한 번은 꼭 써 주세요. 예) LCP 4.3초 → 2.6초, 오류율 3.1% → 1.2%.", fBody));
                }
                if (anns.stream().anyMatch(a -> "vague".equalsIgnoreCase(nz(a.getCategory())))) {
                    doc.add(bullet("• 모호한 말은 행동/방법으로 바꿔 주세요. 예) lazy-loading, code splitting, 캐시 전략 등.", fBody));
                }
                if (anns.stream().anyMatch(a -> "no_jd_match".equalsIgnoreCase(nz(a.getCategory())))) {
                    doc.add(bullet("• 직무 키워드를 1개 자연스럽게 넣어 주세요. 예) "
                            + (jd.isEmpty() ? "Spring Boot, Kafka…" : String.join(", ", jd)) + ".", fBody));
                }
                doc.add(bullet("• STAR 2~3문장 권장: 상황(S) → 행동(A) → 결과(R) 순서로 짧고 선명하게.", fBody));
                doc.add(Spacer(4));

                // 한 줄 STAR 말하기 틀
                doc.add(new Paragraph("한 줄 STAR 말하기 틀", fBold));
                doc.add(codeBlock(
                        "S: [문제/목표]\n" +
                                "A: [구체적 조치 1~2개 + 직무 키워드 1개]\n" +
                                "R: [지표 변화 1개 (전후 수치)]", fMono));
                doc.add(Spacer(4));

                // 예시 답변
                doc.add(new Paragraph("예시 답변", fBold));
                String friendly = rew.isBlank()
                        ? ans
                        : rew.replace("를 활용하여 이미지 최적화를 통해", " 기반으로 이미지 최적화를 적용해")
                        .replace("단축시켰습니다", "낮췄습니다");
                doc.add(codeBlock(friendly, fMono));
                doc.add(Spacer(6));

                // 변경 요약
                doc.add(new Paragraph("무엇이 달라졌나요", fBold));
                String summary = summarizeChanges(ans, rew, anns, jd);
                for (String line : summary.split("\n")) {
                    if (!line.isBlank()) doc.add(bullet("• " + line.trim(), fBody));
                }
                doc.add(Spacer(12));
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF export failed: " + rootMsg(e), e);
        }
    }

    // ---------- Helpers ----------
    private static Paragraph bullet(String text, Font f) { return new Paragraph(text, f); }
    private static Paragraph indent(Paragraph p) { p.setIndentationLeft(14f); return p; }
    private static Paragraph Spacer(float h) { Paragraph p = new Paragraph(" "); p.setSpacingBefore(h/2); p.setSpacingAfter(h/2); return p; }
    private static Paragraph divider() {
        Paragraph p = new Paragraph("────────────────────────────────────────────────────");
        p.setSpacingBefore(6f); p.setSpacingAfter(6f);
        return p;
    }
    private static Paragraph codeBlock(String text, Font mono) {
        Paragraph p = new Paragraph(text == null ? "" : text, mono);
        p.setIndentationLeft(10f);
        p.setSpacingBefore(2f);
        p.setSpacingAfter(2f);
        return p;
    }

    private static String displayCat(String cat) {
        return switch (nz(cat)) {
            case "no_metric"   -> "수치 근거 보강";
            case "vague"       -> "표현 구체화";
            case "no_jd_match" -> "직무 키워드 반영";
            default -> cat;
        };
    }

    // 변경 요약(간단 휴리스틱)
    private String summarizeChanges(String ans, String rew, List<Annotation> anns, List<String> jdInsert) {
        String a = nz(ans), b = nz(rew);
        boolean changed = !stripWs(a).equalsIgnoreCase(stripWs(b));

        StringBuilder sb = new StringBuilder();
        if (!changed) {
            sb.append("내용 변화 없음 (규칙 위반이 경미하거나, 원문을 유지해도 전달력이 충분하다고 판단).");
            return sb.toString();
        }

        boolean aHasMetric = a.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(%|ms|초|점|LCP|CLS|CTR|전환율).*");
        boolean bHasMetric = b.matches("(?s).*\\d+(?:\\.\\d+)?\\s*(%|ms|초|점|LCP|CLS|CTR|전환율).*");

        boolean hadVague = anns.stream().anyMatch(x -> "vague".equalsIgnoreCase(nz(x.getCategory())));
        boolean hadNoMetric = anns.stream().anyMatch(x -> "no_metric".equalsIgnoreCase(nz(x.getCategory())));
        boolean hadNoJd = anns.stream().anyMatch(x -> "no_jd_match".equalsIgnoreCase(nz(x.getCategory())));

        if (hadNoMetric && bHasMetric && !aHasMetric) sb.append("전후 수치를 보강했습니다. ");
        if (hadVague) sb.append("모호한 표현을 구체적 행동/방법으로 바꿨습니다. ");
        if (hadNoJd && !jdInsert.isEmpty()) sb.append("직무 키워드(").append(String.join(", ", jdInsert)).append(")를 자연스럽게 반영했습니다. ");
        if (b.length() != a.length()) sb.append("문장을 더 간결하게 정리했습니다. ");

        if (sb.length() == 0) sb.append("전달력을 높이도록 구조를 다듬었습니다.");
        return sb.toString().trim();
    }

    // 폰트 로더(메모리 → 실패 시 파일 경로 폴백 + 바이트 검증)
    private BaseFont loadBaseFontFromClasspath(String classpathFont) throws Exception {
        ClassPathResource res = new ClassPathResource(classpathFont);
        if (!res.exists()) {
            throw new IllegalStateException("Font not found on classpath: " + classpathFont);
        }
        byte[] ttf;
        try (InputStream is = res.getInputStream()) {
            ttf = is.readAllBytes();
        }
        System.out.println("[PDF] Font bytes = " + (ttf == null ? 0 : ttf.length));
        if (ttf == null || ttf.length < 200_000) {
            throw new IllegalStateException("Font bytes too small or invalid. Check file at " + classpathFont);
        }

        try {
            return BaseFont.createFont("NotoSansKR-Regular.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, ttf, null);
        } catch (com.lowagie.text.DocumentException ex) {
            System.err.println("[PDF] Memory load failed, fallback to temp file: " + ex.getMessage());
            File tmp = File.createTempFile("NotoSansKR-Regular-", ".ttf");
            Files.write(tmp.toPath(), ttf);
            tmp.deleteOnExit();
            return BaseFont.createFont(tmp.getAbsolutePath(),
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
    }

    // utils
    private static String nz(String s){ return s == null ? "" : s; }
    private static <T> List<T> nlist(List<T> xs){ return xs == null ? List.of() : xs; }
    private static String stripWs(String s){ return s == null ? "" : s.replaceAll("\\s+",""); }
    private static String rootMsg(Throwable t){ Throwable r=t; while(r.getCause()!=null) r=r.getCause(); return Objects.toString(r.getMessage(), r.getClass().getName()); }
}

package ict.project.feedback.core;

import ict.project.feedback.api.dto.FeedbackItem;
import ict.project.feedback.api.dto.FeedbackResponse;

import java.util.List;
import java.util.stream.Collectors;

public class PromptFactory {

    /** 1차 기본 시스템 프롬프트: JSON만, STAR 3문장, 원문 복사 금지(20%+) */
    public static String system() {
        return """
        당신은 면접 답변을 전문적으로 다듬는 에디터입니다.
        아래 규칙을 모두 지키고, 지정한 JSON 스키마의 '순수 JSON'만 반환하세요. 
        (마크다운/설명/코드펜스/추가 키 금지)

        [출력 스키마]
        {
          "rewrite": "string",
          "jdInsert": ["string"]
        }

        [편집 규칙]
        1) 원문 그대로 복사 금지: 의미는 유지하되 최소 20%% 이상 어휘/표현을 변경합니다.
        2) STAR 3문장으로 재구성:
           - 1문장: 상황/과제(S/T) 요약
           - 1문장: 행동(A) 핵심(무엇을 어떻게)
           - 1문장: 결과(R) 수치/임팩트(전후 비교)
        3) JD 키워드 최소 1개를 핵심 문장에 자연스럽게 포함합니다.
        4) 수치/단위 통일: 예) 4.3s → 4.3초, 40%% 등. 화살표(→)는 서술형으로 바꿉니다.
        5) 길이: 한국어 기준 120~200자.
        6) 톤: 능동태, 간결·명확. 중복/군더더기 제거.
        7) annotations의 comment/suggest를 반영하되 과장/허위는 금지합니다.

        [검증]
        아래 중 하나라도 만족 못 하면 동일 출력 금지, 다시 작성:
        - JD 키워드 미포함
        - 수치/단위 개선 없음
        - STAR 3문장 아님
        - 원문과 어휘 차이가 20%% 미만

        반드시 위 스키마의 JSON만 반환하세요.
        """;
    }

    /** user 프롬프트: 체크리스트/키워드/원문/룰 적용 근거(annotations) 모두 제공 */
    public static String user(FeedbackResponse ctx, FeedbackItem item, List<String> jdKeywords) {
        String checklist = (ctx != null && ctx.getChecklist() != null)
                ? ctx.getChecklist().stream().map(s -> "- " + s).collect(Collectors.joining("\n"))
                : "- (none)";

        String jd = (jdKeywords == null || jdKeywords.isEmpty())
                ? "(none)"
                : String.join(", ", jdKeywords);

        String ann = summarizeAnnotations(item);

        return """
        다음 입력을 위 규칙에 따라 '순수 JSON'만으로 반환하세요.

        [체크리스트]
        %s

        [JD 키워드]
        %s

        [원문 답변(answer)]
        %s

        [규칙 탐지(annotations) 요약]
        %s
        """.formatted(checklist, jd, safe(item != null ? item.getAnswer() : ""), ann);
    }

    /* ===========================
       선택: 무변경(no-op) 시 2차 패스용
       =========================== */

    /** 더 강한 시스템 프롬프트(무변경 방지): 30%+, 구조 재배치 강제 */
    public static String systemStrict() {
        return """
        원문 복사 금지 강제 모드입니다. '순수 JSON'만 반환하세요.
        스키마: { "rewrite":"string", "jdInsert":["string"] }

        - 의미는 유지하되 최소 30%% 이상 어휘 변경 및 문장 구조 재배치
        - STAR 3문장 미충족/ JD 키워드 미삽입 시 무조건 재작성
        - 수치/단위 통일(초, % 등), 길이 120~200자, 능동태
        """;
    }

    public static String userStrict(FeedbackResponse ctx, FeedbackItem item, List<String> jdKeywords) {
        String jd = (jdKeywords == null || jdKeywords.isEmpty())
                ? "(none)"
                : String.join(", ", jdKeywords);
        return """
        [JD 키워드] %s

        [원문]
        %s

        [필수 변경]
        - 어휘/표현 30%%+ 치환, STAR 3문장, JD 키워드 1개+
        - 단위/수치 통일(초/%%), 기호(→)는 서술형으로 변경
        """.formatted(jd, safe(item != null ? item.getAnswer() : ""));
    }

    /* ===========================
       유틸
       =========================== */

    private static String summarizeAnnotations(FeedbackItem item) {
        if (item == null || item.getAnnotations() == null || item.getAnnotations().isEmpty()) {
            return "- (none)";
        }
        return item.getAnnotations().stream().map(a -> {
            String span = a.getSpan() != null ? a.getSpan().getText() : "";
            String comment = safe(a.getComment());
            String suggest = safe(a.getSuggest());
            String cat = safe(a.getCategory());
            String spanPart = span.isBlank() ? "" : " [span=\"" + span + "\"]";
            String suggestPart = suggest.isBlank() ? "" : " | suggest: " + suggest;
            return "- " + cat + ": " + comment + spanPart + suggestPart;
        }).collect(Collectors.joining("\n"));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

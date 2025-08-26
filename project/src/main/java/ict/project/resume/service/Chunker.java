package ict.project.resume.service;

import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 긴 텍스트를 문장 경계 기준으로 모아서 maxLen 이하의 청크들로 만드는 유틸.
 * - 한국어/영어 문장 경계: BreakIterator 사용
 * - 한 문장이 maxLen을 초과하면 hard split
 */
@Service
public class Chunker {

    /** 문장 리스트로만 받고 싶을 때 사용(옵션) */
    public List<String> splitIntoSentences(String text) {
        List<String> sents = new ArrayList<>();
        if (text == null || text.isBlank()) return sents;

        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.KOREAN);
        bi.setText(text);
        int start = bi.first();
        for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) sents.add(sentence);
        }
        return sents;
    }

    /**
     * 문장들을 묶어서 길이 ≤ maxLen 의 청크로 패킹
     */
    public List<String> chunk(String text, int maxLen) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;

        if (maxLen <= 0) {
            out.add(text.trim());
            return out;
        }

        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            // 문장 경계 못 찾으면 통으로 자르기
            hardSplit(text.trim(), maxLen, out);
            return out;
        }

        StringBuilder buf = new StringBuilder();
        for (String sent : sentences) {
            if (sent.length() > maxLen) {
                // 현재 버퍼를 먼저 내보내고, 긴 문장은 강제 분할
                if (buf.length() > 0) {
                    out.add(buf.toString());
                    buf.setLength(0);
                }
                hardSplit(sent, maxLen, out);
                continue;
            }

            if (buf.length() == 0) {
                buf.append(sent);
            } else if (buf.length() + 1 + sent.length() <= maxLen) {
                buf.append(' ').append(sent);
            } else {
                out.add(buf.toString());
                buf.setLength(0);
                buf.append(sent);
            }
        }
        if (buf.length() > 0) out.add(buf.toString());
        return out;
    }

    /** 한 문장이 maxLen을 넘는 경우 강제 슬라이스 */
    private void hardSplit(String s, int maxLen, List<String> out) {
        int i = 0, n = s.length();
        while (i < n) {
            int end = Math.min(n, i + maxLen);
            out.add(s.substring(i, end));
            i = end;
        }
    }
}

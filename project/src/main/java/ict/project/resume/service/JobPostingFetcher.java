package ict.project.resume.service;

public interface JobPostingFetcher {
    /**
     * 채용공고 URL에서 사람 읽기용 본문 텍스트를 추출해 반환합니다.
     * 실패 시 빈 문자열 반환.
     */
    String fetch(String url);
}

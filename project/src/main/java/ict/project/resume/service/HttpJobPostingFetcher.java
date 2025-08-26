package ict.project.resume.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class HttpJobPostingFetcher implements JobPostingFetcher {

    private final WebClient client = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Safari/537.36")
            .build();

    @Override
    public String fetch(String url) {
        if (url == null || url.isBlank()) return "";

        String html = client.get()
                .uri(url)
                .accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML, MediaType.ALL)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(15));

        if (html == null || html.isBlank()) return "";

        // Jsoup이 있으면 사용, 없으면 간단 폴백
        try {
            return htmlToTextWithJsoup(html);
        } catch (NoClassDefFoundError | Exception ignore) {
            return htmlToTextFallback(html);
        }
    }

    // ---------------- helpers ----------------

    private String htmlToTextWithJsoup(String html) throws Exception {
        // Jsoup 의존성 필요: implementation "org.jsoup:jsoup:1.17.2" (또는 최신)
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
        // 텍스트에 방해되는 요소 제거
        doc.select("script,style,noscript,svg,canvas,iframe,header,footer,nav,form,aside").remove();

        String title = safe(doc.title());
        String body = doc.body() != null ? safe(doc.body().text()) : "";

        String combined = (title + "\n\n" + body).trim();
        return squeeze(combined);
    }

    /** Jsoup 미존재 시 단순 태그 제거 폴백(정확도 낮음) */
    private String htmlToTextFallback(String html) {
        // 태그 제거
        String text = html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<!--.*?-->", " ")
                .replaceAll("(?is)<[^>]+>", " ");
        return squeeze(text);
    }

    private static String squeeze(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

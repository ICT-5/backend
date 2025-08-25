package ict.project.resumeAnalyze;

import ict.project.resumeAnalyze.dto.InputRequestDto;
import ict.project.resumeAnalyze.dto.ResumeQuestionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResumeAnalyzeService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.api-url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    public List<ResumeQuestionDto> generateQuestions(InputRequestDto request) {
        log.info("🔑 OpenAI Key: {}", apiKey);
        RestTemplate restTemplate = new RestTemplate();

        String prompt = buildPrompt(request.getResumeText(), request.getJobPostText());

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(message));
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        log.info("🔑 OpenAI Key: {}", apiKey);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, httpEntity, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            String content = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            // 질문 한 줄씩 분리
            String[] lines = content.split("\n");
            List<ResumeQuestionDto> questions = new ArrayList<>();
            for (String line : lines) {
                String cleaned = line.replaceAll("^[0-9]+[.)]?\\s*", "").trim();
                if (!cleaned.isEmpty()) {
                    questions.add(new ResumeQuestionDto(cleaned));
                }
            }

            return questions;

        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String resume, String jobPost) {
        return """
            아래는 이력서와 채용공고입니다.

            [이력서]
            %s

            [채용공고]
            %s

            위 내용을 기반으로 면접 예상 질문 20개를 한국어로 출력해 주세요.
            각 질문은 한 줄로 출력하고, 번호를 붙여 주세요.
            이력서와 채용공고는 각 항목을 매칭해서 꼼꼼히 비교하고,
            두 내용을 바탕으로 지원자의 직무, 경력, 스펙 등을 추출하여 그 기반으로 질문을 뽑아주세요.
            """.formatted(resume, jobPost);
    }
}

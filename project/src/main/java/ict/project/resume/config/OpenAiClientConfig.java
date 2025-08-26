// OpenAiClientConfig.java
package ict.project.resume.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

// OpenAiClientConfig.java
@Configuration
public class OpenAiClientConfig {
    @Bean("openaiWebClient")
    public WebClient openaiWebClient(WebClient.Builder builder,
                                     @Value("${openai.baseUrl:https://api.openai.com/v1}") String baseUrl,
                                     @Value("${openai.apiKey}") String apiKey) {
        return builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}

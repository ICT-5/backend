// WebClientConfig.java (Chroma)
package ict.project.resume.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// WebClientConfig.java
@Configuration
public class WebClientConfig {
    @Bean("chromaWebClient")
    public WebClient chromaWebClient(WebClient.Builder builder,
                                     @Value("${chroma.baseUrl:http://localhost:8000}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}


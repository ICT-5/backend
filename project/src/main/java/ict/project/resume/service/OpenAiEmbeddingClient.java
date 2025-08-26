// src/main/java/ict/project/resume/service/OpenAiEmbeddingClient.java
package ict.project.resume.service;

import java.util.List;

public interface OpenAiEmbeddingClient {
    /** OpenAI Embeddings 호출: model, inputs(문자열 리스트) -> 각 문장당 float[] 벡터 */
    List<float[]> embed(String model, List<String> inputs);
}

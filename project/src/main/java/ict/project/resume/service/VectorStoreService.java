package ict.project.resume.service;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {
    void ensureCollection(String name);

    void upsert(String collection, String id, float[] vector, Map<String, Object> metadata, String text);

    List<SearchResult> query(String collection, float[] vector, int topK);

    record SearchResult(String id, double distance, Map<String,Object> metadata, String text) {}
}

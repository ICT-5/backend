package ict.project.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TextExtractionService {
    public String extract(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            parser.parse(in, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (Exception e) {
            throw new RuntimeException("텍스트 추출 실패: " + e.getMessage(), e);
        }
    }
}



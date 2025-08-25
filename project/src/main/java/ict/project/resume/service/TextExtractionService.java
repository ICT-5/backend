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

@Slf4j
@Service
public class TextExtractionService {
    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    public String extract(Path path) throws Exception {
        Path p = path;
        if (!Files.exists(p)) {
            p = Path.of("").toAbsolutePath().resolve(path.toString()).normalize();
        }
        if (!Files.exists(p)) throw new IllegalArgumentException("파일을 찾을 수 없습니다: " + path);

        log.debug("Tika detect: {} ({})", tika.detect(p), p);
        try (InputStream is = Files.newInputStream(p)) {
            BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024); // up to ~10MB text
            Metadata md = new Metadata();
            ParseContext ctx = new ParseContext();
            parser.parse(is, handler, md, ctx);
            String text = handler.toString();
            return text == null ? "" : text.replace("\u0000","").trim();
        }
    }
}


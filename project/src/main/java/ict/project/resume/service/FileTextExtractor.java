package ict.project.resume.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileTextExtractor {

    public String extract(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "";

        try (InputStream is = file.getInputStream()) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();

            // 파일명/콘텐츠 타입 힌트 주기 (버전 호환 위해 문자열 키 사용)
            if (file.getOriginalFilename() != null) {
                metadata.set("resourceName", file.getOriginalFilename()); // ✅ 버전 무관
                // 만약 상수를 쓰고 싶다면 (둘 중 하나만 택):
                // metadata.set(org.apache.tika.metadata.Metadata.RESOURCE_NAME_KEY, file.getOriginalFilename());
                // metadata.set(org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            }
            if (file.getContentType() != null) {
                metadata.set(Metadata.CONTENT_TYPE, file.getContentType());
            }

            ParseContext context = new ParseContext();
            parser.parse(is, handler, metadata, context);
            return handler.toString();
        } catch (SAXException | org.apache.tika.exception.TikaException e) {
            throw new IOException("텍스트 추출 실패", e);
        }
    }
}


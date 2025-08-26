//// src/main/java/ict/project/resume/service/BulkExcelIngestService.java
//package ict.project.resume.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.stereotype.Service;
//
//import java.io.InputStream;
//import java.util.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class BulkExcelIngestService {
//
//    private final Chunker chunker;
//    private final EmbeddingService embeddingService;
//    private final ChromaService chromaService;
//
//    /** 기존 호환용 (maxLen 기본 1200) */
//    public int ingest(InputStream excelStream, String sourceTag) throws Exception {
//        return ingest(excelStream, sourceTag, 1200);
//    }
//
//    /** 엑셀 → 청킹(maxLen) → 임베딩 → Chroma upsert */
//    public int ingest(InputStream excelStream, String sourceTag, int maxLen) throws Exception {
//        int totalChunks = 0;
//        try (Workbook wb = new XSSFWorkbook(excelStream)) {
//            Sheet sheet = wb.getSheetAt(0);
//            if (sheet == null) return 0;
//
//            final String collection = "resumes";
//
//            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
//                Row row = sheet.getRow(r);
//                if (row == null) continue;
//
//                String essayId   = getString(row, 0); // A열: ID
//                String essayText = getString(row, 1); // B열: 본문
//                if (essayText == null || essayText.isBlank()) continue;
//
//                List<String> chunks = chunker.chunk(essayText, maxLen);
//
//                int idx = 0;
//                for (String ch : chunks) {
//                    List<Float> vec = embeddingService.embedAsList(ch);
//                    String id = (essayId == null || essayId.isBlank() ? ("row-" + r) : essayId) + ":" + (idx++);
//
//                    Map<String, Object> meta = new HashMap<>();
//                    meta.put("essay_id", essayId);
//                    meta.put("source", sourceTag);
//                    meta.put("chunk_len", ch.length());
//                    meta.put("row_index", r);
//
//                    chromaService.upsert(collection, id, ch, vec, meta);
//                    totalChunks++;
//                }
//            }
//        }
//        log.info("Excel ingestion done. totalChunks={}, sourceTag={}, maxLen={}", totalChunks, sourceTag, maxLen);
//        return totalChunks;
//    }
//
//    private String getString(Row row, int colIdx) {
//        Cell cell = row.getCell(colIdx);
//        if (cell == null) return null;
//        cell.setCellType(CellType.STRING);
//        String s = cell.getStringCellValue();
//        return (s == null) ? null : s.trim();
//    }
//}



// src/main/java/ict/project/resume/service/BulkExcelIngestService.java
package ict.project.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkExcelIngestService {

    private final Chunker chunker;
    private final EmbeddingService embeddingService;
    private final ChromaService chromaService;

    /** 기존 호환용 (maxLen 기본 1200) */
    public int ingest(InputStream excelStream, String sourceTag) throws Exception {
        return ingest(excelStream, sourceTag, 1200);
    }

    /** 엑셀 → 청킹(maxLen) → 임베딩 → Chroma upsert */
    public int ingest(InputStream excelStream, String sourceTag, int maxLen) throws Exception {
        int totalChunks = 0;
        try (Workbook wb = new XSSFWorkbook(excelStream)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return 0;

            final String collection = "resumes";

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String essayId   = getString(row, 0); // A열: ID
                String essayText = getString(row, 1); // B열: 본문
                if (essayText == null || essayText.isBlank()) continue;

                List<String> chunks = chunker.chunk(essayText, maxLen);

                int idx = 0;
                for (String ch : chunks) {
                    List<Float> vec = embeddingService.embedAsList(ch);
                    String id = (essayId == null || essayId.isBlank() ? ("row-" + r) : essayId) + ":" + (idx++);

                    Map<String, Object> meta = new HashMap<>();
                    meta.put("essay_id", essayId);
                    meta.put("source", sourceTag);
                    meta.put("chunk_len", ch.length());
                    meta.put("row_index", r);

                    chromaService.upsert(collection, id, ch, vec, meta);
                    totalChunks++;
                }
            }
        }
        log.info("Excel ingestion done. totalChunks={}, sourceTag={}, maxLen={}", totalChunks, sourceTag, maxLen);
        return totalChunks;
    }

    private String getString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String s = cell.getStringCellValue();
        return (s == null) ? null : s.trim();
    }
}

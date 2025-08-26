package ict.project.resume.service;

import ict.project.resume.entity.RagChunkEntity;
import ict.project.resume.entity.RagSettingsEntity;
import ict.project.resume.repository.RagChunkRepository;
import ict.project.resume.repository.RagSettingsRepository;
import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RagChunkRepository ragChunkRepository;
    private final RagSettingsRepository ragSettingsRepository;
    private final UserRepository userRepository;

    /** 전역 코퍼스를 저장할 관리자 User.id (application.properties) */
    @Value("${app.rag.admin-user-id:1}")
    private Integer adminUserId;

    /** 청크 분할 기본값 (문자 기준) */
    private static final int MAX_CHARS_PER_CHUNK = 1500;

    /** ✅ 프롬프트 전체 상한(문자). 대략 1토큰≈4문자 가정 → 120k chars ≈ 30k tokens */
    private static final int MAX_PROMPT_CHARS = 120_000;

    /** ✅ 섹션별 최대 청크 수 (필요시 조정) */
    private static final int MAX_CORPUS_CHUNKS  = 8;
    private static final int MAX_RESUME_CHUNKS  = 10;
    private static final int MAX_POSTING_CHUNKS = 8;

    /* ============================= */
    /* ======= 공용 유틸 영역 ======= */
    /* ============================= */

    private List<String> splitToChunks(String content, int maxChars) {
        List<String> out = new ArrayList<>();
        if (content == null) return out;

        String normalized = content.replaceAll("\\r", "");
        int len = normalized.length();
        for (int i = 0; i < len; i += maxChars) {
            int end = Math.min(len, i + maxChars);
            String piece = normalized.substring(i, end).trim();
            if (!piece.isEmpty()) out.add(piece);
        }
        return out;
    }

    private Optional<UserEntity> getAdminUserIfConfigured() {
        if (adminUserId == null || adminUserId <= 0) return Optional.empty();
        return userRepository.findById(adminUserId);
    }

    /** ✅ 남은 예산 안에서 청크를 append. 초과 시 자르고, 섹션별 개수 제한. */
    private int appendChunksCapped(StringBuilder sb,
                                   List<RagChunkEntity> chunks,
                                   String sectionTitle,
                                   int maxChunks,
                                   int remainingBudget) {
        if (chunks == null || chunks.isEmpty() || remainingBudget <= 0) return 0;

        int used = 0;
        sb.append("## [").append(sectionTitle).append("]\n");
        int count = 0;

        for (RagChunkEntity c : chunks) {
            if (count >= maxChunks || remainingBudget - used <= 0) break;

            String text = c.getContent();
            if (text == null || text.isBlank()) continue;
            String line = "- " + text + "\n\n";

            int room = remainingBudget - used;
            if (line.length() <= room) {
                sb.append(line);
                used += line.length();
            } else {
                // 마지막 남은 예산만큼 잘라서 붙이고 종료
                sb.append(line, 0, Math.max(0, room));
                used += Math.max(0, room);
                break;
            }
            count++;
        }

        if (count == 0) {
            sb.append("(데이터가 없습니다.)\n\n");
        }
        return used;
    }

    /* ========================================= */
    /* ======= rag_settings (프롬프트) 영역 ======= */
    /* ========================================= */

    /** 전역 이력서 피드백 프롬프트 보장 */
    @Transactional
    public void ensureResumeFeedbackPrompt() {
        ragSettingsRepository.findByName("resume_feedback").orElseGet(() -> {
            RagSettingsEntity s = new RagSettingsEntity();
            s.setName("resume_feedback");
            s.setPromptText("합격자 자소서들이야 이걸 토대로 앞으로의 사용자들의 자소서를 피드백해줘");
            s.setUpdatedAt(LocalDateTime.now());
            return ragSettingsRepository.save(s);
        });
    }

    /** 전역 프롬프트 엔티티 반환 */
    @Transactional
    public RagSettingsEntity getResumeFeedbackSetting() {
        return ragSettingsRepository.findByName("resume_feedback")
                .orElseThrow(() -> new IllegalStateException("resume_feedback 프롬프트가 세팅되지 않았습니다."));
    }

    /* ========================================= */
    /* ======= CORPUS(전역) 관련 공개 메서드 ======= */
    /* ========================================= */

    @Transactional
    public List<RagChunkEntity> registerCorpusChunks(Integer adminUserId, String filePath, String content) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
        }
        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));

        List<String> chunks = splitToChunks(content, MAX_CHARS_PER_CHUNK);
        List<RagChunkEntity> saved = new ArrayList<>();

        for (String chunk : chunks) {
            RagChunkEntity entity = new RagChunkEntity();
            entity.setUser(admin);
            entity.setSource("CORPUS");
            entity.setFilePath(filePath);
            entity.setContent(chunk);
            entity.setEmbeddingJson("[]"); // 임시
            entity.setCreatedAt(LocalDateTime.now());

            try {
                saved.add(ragChunkRepository.save(entity));
            } catch (Exception e) {
                log.warn("중복 또는 저장 실패로 스킵: {}", e.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public List<RagChunkEntity> getCorpusChunks(Integer adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
        }
        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));
        return ragChunkRepository.findByUserAndSource(admin, "CORPUS");
    }

    @Transactional
    public int deleteCorpusChunks(Integer adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
        }
        UserEntity admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));
        List<RagChunkEntity> all = ragChunkRepository.findByUserAndSource(admin, "CORPUS");
        ragChunkRepository.deleteAllInBatch(all);
        return all.size();
    }

    /* ========================================= */
    /* ======= 분석 프롬프트 구성(핵심 메서드) ======= */
    /* ========================================= */

    /** includePosting 기본 false */
    @Transactional
    public String buildFeedbackPrompt(Integer userId) {
        return buildFeedbackPrompt(userId, false);
    }

    /**
     * ✅ 전역 CORPUS(관리자) + 사용자 RESUME(+POSTING, 옵션) 합쳐서 프롬프트 구성
     *    - 전체 길이 상한과 섹션별 최대 청크 수를 적용해 TPM/컨텍스트 초과를 방지
     */
    @Transactional
    public String buildFeedbackPrompt(Integer userId, boolean includePosting) {
        RagSettingsEntity prompt = ragSettingsRepository.findByName("resume_feedback")
                .orElseThrow(() -> new IllegalStateException("resume_feedback 프롬프트가 세팅되지 않았습니다."));

        StringBuilder sb = new StringBuilder(Math.min(2048, MAX_PROMPT_CHARS));
        int used = 0;

        // (0) 시스템 지침
        String header = prompt.getPromptText() + "\n\n";
        if (header.length() > MAX_PROMPT_CHARS) {
            sb.append(header, 0, MAX_PROMPT_CHARS);
            return sb.toString();
        }
        sb.append(header);
        used += header.length();

        int budget = MAX_PROMPT_CHARS - used;

        // (A) 전역 CORPUS (관리자)
        if (budget > 0) {
            getAdminUserIfConfigured().ifPresent(admin -> {
                List<RagChunkEntity> corpus = ragChunkRepository.findByUserAndSource(admin, "CORPUS");
                // 필요시 최신/파일별 정렬 등 추가 가능
                int added = appendChunksCapped(sb, corpus, "전역 코퍼스", MAX_CORPUS_CHUNKS, MAX_PROMPT_CHARS - sb.length());
                log.debug("append corpus used chars={}", added);
            });
        }

        // (B) 사용자 RESUME
        if (budget > 0) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));
            List<RagChunkEntity> resumes = ragChunkRepository.findByUserAndSource(user, "RESUME");
            int added = appendChunksCapped(sb, resumes, "사용자 이력서", MAX_RESUME_CHUNKS, MAX_PROMPT_CHARS - sb.length());
            log.debug("append resume used chars={}", added);

            // (C) 사용자 POSTING (옵션)
            if (includePosting && (MAX_PROMPT_CHARS - sb.length()) > 0) {
                List<RagChunkEntity> postings = ragChunkRepository.findByUserAndSource(user, "POSTING");
                added = appendChunksCapped(sb, postings, "사용자 채용공고", MAX_POSTING_CHUNKS, MAX_PROMPT_CHARS - sb.length());
                log.debug("append posting used chars={}", added);
            }
        }

        // 남은 예산 0이면 더 이상 붙이지 않음
        return sb.substring(0, Math.min(sb.length(), MAX_PROMPT_CHARS));
    }

    /* ========================= 저장 유틸 ========================= */

    @Transactional
    public List<RagChunkEntity> registerUserChunks(Integer userId, String source, String filePath, String content) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId가 올바르지 않습니다.");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다. id=" + userId));

        List<String> chunks = splitToChunks(content, MAX_CHARS_PER_CHUNK);
        List<RagChunkEntity> saved = new ArrayList<>();

        for (String chunk : chunks) {
            RagChunkEntity e = new RagChunkEntity();
            e.setUser(user);
            e.setSource(source);          // "RESUME" / "POSTING" 등
            e.setFilePath(filePath);
            e.setContent(chunk);
            e.setEmbeddingJson("[]");     // 임시(임베딩 미적용)
            e.setCreatedAt(LocalDateTime.now());

            try {
                saved.add(ragChunkRepository.save(e));
            } catch (Exception ex) {
                log.warn("RAG 저장 스킵(중복/오류): {}", ex.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public List<RagChunkEntity> registerResumeChunks(Integer userId, String filePath, String content) {
        return registerUserChunks(userId, "RESUME", filePath, content);
    }

    @Transactional
    public List<RagChunkEntity> registerResumeChunks(Integer userId,
                                                     ict.project.resume.entity.ResumeEntity saved,
                                                     String content,
                                                     String source) {
        String filePath = "RESUME".equalsIgnoreCase(source) ? saved.getFilePath() : saved.getJobfilePath();
        return registerUserChunks(userId, source, filePath, content);
    }
}

//package ict.project.service;
//
//import ict.project.entity.RagChunkEntity;
//import ict.project.entity.RagSettingsEntity;
//import ict.project.entity.UserEntity;
//import ict.project.repository.RagChunkRepository;
//import ict.project.repository.RagSettingsRepository;
//import ict.project.repository.UserRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RagService {
//
//    private final RagChunkRepository ragChunkRepository;
//    private final RagSettingsRepository ragSettingsRepository;
//    private final UserRepository userRepository;
//
//    /** 전역 코퍼스를 저장할 관리자 User.id (application.properties) */
//    @Value("${app.rag.admin-user-id:1}")
//    private Integer adminUserId;
//
//    /** 청크 분할 기본값 (문자 기준) */
//    private static final int MAX_CHARS_PER_CHUNK = 1500;
//
//    /* ============================= */
//    /* ======= 공용 유틸 영역 ======= */
//    /* ============================= */
//
//    private List<String> splitToChunks(String content, int maxChars) {
//        List<String> out = new ArrayList<>();
//        if (content == null) return out;
//
//        String normalized = content.replaceAll("\\r", "");
//        int len = normalized.length();
//        for (int i = 0; i < len; i += maxChars) {
//            int end = Math.min(len, i + maxChars);
//            String piece = normalized.substring(i, end).trim();
//            if (!piece.isEmpty()) out.add(piece);
//        }
//        return out;
//    }
//
//    private Optional<UserEntity> getAdminUserIfConfigured() {
//        if (adminUserId == null || adminUserId <= 0) return Optional.empty();
//        return userRepository.findById(adminUserId);
//    }
//
//    protected void appendChunks(StringBuilder sb, List<RagChunkEntity> chunks) {
//        for (RagChunkEntity c : chunks) {
//            sb.append("- ").append(c.getContent()).append("\n\n");
//        }
//    }
//
//    /* ========================================= */
//    /* ======= rag_settings (프롬프트) 영역 ======= */
//    /* ========================================= */
//
//    /** 전역 이력서 피드백 프롬프트 보장 */
//    @Transactional
//    public void ensureResumeFeedbackPrompt() {
//        ragSettingsRepository.findByName("resume_feedback").orElseGet(() -> {
//            RagSettingsEntity s = new RagSettingsEntity();
//            s.setName("resume_feedback");
//            s.setPromptText("합격자 자소서들이야 이걸 토대로 앞으로의 사용자들의 자소서를 피드백해줘");
//            s.setUpdatedAt(LocalDateTime.now());
//            return ragSettingsRepository.save(s);
//        });
//    }
//
//    /** 전역 프롬프트 엔티티 반환 (컨트롤러에서 OK 응답용) */
//    @Transactional
//    public RagSettingsEntity getResumeFeedbackSetting() {
//        return ragSettingsRepository.findByName("resume_feedback")
//                .orElseThrow(() -> new IllegalStateException("resume_feedback 프롬프트가 세팅되지 않았습니다."));
//    }
//
//    /* ========================================= */
//    /* ======= CORPUS(전역) 관련 공개 메서드 ======= */
//    /* ========================================= */
//
//    /**
//     * 전역 코퍼스 텍스트를 분할하여 RAG 청크로 저장 (source="CORPUS", user=adminUserId)
//     * @param adminUserId 관리자 사용자 ID
//     * @param filePath    업로드(또는 가짜) 경로
//     * @param content     추출/입력된 전체 텍스트
//     */
//    @Transactional
//    public List<RagChunkEntity> registerCorpusChunks(Integer adminUserId, String filePath, String content) {
//        if (adminUserId == null || adminUserId <= 0) {
//            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
//        }
//        UserEntity admin = userRepository.findById(adminUserId)
//                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));
//
//        List<String> chunks = splitToChunks(content, MAX_CHARS_PER_CHUNK);
//        List<RagChunkEntity> saved = new ArrayList<>();
//
//        for (String chunk : chunks) {
//            RagChunkEntity entity = new RagChunkEntity();
//            entity.setUser(admin);
//            entity.setSource("CORPUS");
//            entity.setFilePath(filePath);
//            entity.setContent(chunk);
//            entity.setEmbeddingJson("[]"); // 임시(임베딩 미적용)
//            entity.setCreatedAt(LocalDateTime.now());
//
//            try {
//                saved.add(ragChunkRepository.save(entity));
//            } catch (Exception e) {
//                // (유니크 키 충돌 등) 이미 동일 내용이 있으면 스킵
//                log.warn("중복 또는 저장 실패로 스킵: {}", e.getMessage());
//            }
//        }
//        return saved;
//    }
//
//    /** 전역 코퍼스 청크 조회 */
//    @Transactional
//    public List<RagChunkEntity> getCorpusChunks(Integer adminUserId) {
//        if (adminUserId == null || adminUserId <= 0) {
//            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
//        }
//        UserEntity admin = userRepository.findById(adminUserId)
//                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));
//        return ragChunkRepository.findByUserAndSource(admin, "CORPUS");
//    }
//
//    /** 전역 코퍼스 청크 전체 삭제 (반환: 삭제 개수) */
//    @Transactional
//    public int deleteCorpusChunks(Integer adminUserId) {
//        if (adminUserId == null || adminUserId <= 0) {
//            throw new IllegalArgumentException("관리자 ID가 올바르지 않습니다.");
//        }
//        UserEntity admin = userRepository.findById(adminUserId)
//                .orElseThrow(() -> new IllegalStateException("관리자 사용자가 존재하지 않습니다. id=" + adminUserId));
//        List<RagChunkEntity> all = ragChunkRepository.findByUserAndSource(admin, "CORPUS");
//        ragChunkRepository.deleteAllInBatch(all);
//        return all.size();
//    }
//
//    /* ========================================= */
//    /* ======= 분석 프롬프트 구성(핵심 메서드) ======= */
//    /* ========================================= */
//
//    /**
//     * 전역 CORPUS(관리자) + 사용자 RESUME(+POSTING, 옵션) 합쳐서 프롬프트 구성
//     * @param userId 분석 대상 사용자
//     * @param includePosting 채용공고 포함 여부 (false면 미포함)
//     */
//    @Transactional
//    public String buildFeedbackPrompt(Integer userId, boolean includePosting) {
//        RagSettingsEntity prompt = ragSettingsRepository.findByName("resume_feedback")
//                .orElseThrow(() -> new IllegalStateException("resume_feedback 프롬프트가 세팅되지 않았습니다."));
//
//        StringBuilder sb = new StringBuilder(2048);
//        sb.append(prompt.getPromptText()).append("\n\n");
//
//        // (A) 전역 CORPUS (adminUserId 소유)
//        getAdminUserIfConfigured().ifPresent(admin -> {
//            List<RagChunkEntity> corpus = ragChunkRepository.findByUserAndSource(admin, "CORPUS");
//            if (!corpus.isEmpty()) {
//                sb.append("## [전역 코퍼스]\n");
//                appendChunks(sb, corpus);
//            }
//        });
//
//        // (B) 해당 사용자의 RESUME
//        UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));
//        List<RagChunkEntity> resumes = ragChunkRepository.findByUserAndSource(user, "RESUME");
//        if (!resumes.isEmpty()) {
//            sb.append("## [사용자 이력서]\n");
//            appendChunks(sb, resumes);
//        } else {
//            sb.append("## [사용자 이력서]\n(등록된 이력서가 없습니다.)\n\n");
//        }
//
//        // (C) 채용공고는 includePosting==true 인 경우에만 “해당 사용자” 것만 포함
//        if (includePosting) {
//            List<RagChunkEntity> postings = ragChunkRepository.findByUserAndSource(user, "POSTING");
//            if (!postings.isEmpty()) {
//                sb.append("## [사용자 채용공고]\n");
//                appendChunks(sb, postings);
//            }
//        }
//        return sb.toString();
//    }
//
//    /** (선택) 기존 1-인자 호출 호환용 오버로드: 기본은 채용공고 미포함 */
//    @Transactional
//    public String buildFeedbackPrompt(Integer userId) {
//        return buildFeedbackPrompt(userId, false);
//    }
//
//    // === 공용: userId + source + filePath + content 로 청크 저장 ===
//    @Transactional
//    public List<RagChunkEntity> registerUserChunks(Integer userId, String source, String filePath, String content) {
//        if (userId == null || userId <= 0) {
//            throw new IllegalArgumentException("userId가 올바르지 않습니다.");
//        }
//        UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다. id=" + userId));
//
//        List<String> chunks = splitToChunks(content, MAX_CHARS_PER_CHUNK);
//        List<RagChunkEntity> saved = new ArrayList<>();
//
//        for (String chunk : chunks) {
//            RagChunkEntity e = new RagChunkEntity();
//            e.setUser(user);
//            e.setSource(source);          // "RESUME" / "POSTING" 등
//            e.setFilePath(filePath);
//            e.setContent(chunk);
//            e.setEmbeddingJson("[]");     // 임시(임베딩 미적용)
//            e.setCreatedAt(java.time.LocalDateTime.now());
//
//            try {
//                saved.add(ragChunkRepository.save(e));
//            } catch (Exception ex) {
//                // UNIQUE 제약(uq_rag_unique) 충돌 등은 스킵
//                log.warn("RAG 저장 스킵(중복/오류): {}", ex.getMessage());
//            }
//        }
//        return saved;
//    }
//
//    // === 편의: 이력서 전용 (파일 경로만 넘겨 저장) ===
//    @Transactional
//    public List<RagChunkEntity> registerResumeChunks(Integer userId, String filePath, String content) {
//        return registerUserChunks(userId, "RESUME", filePath, content);
//    }
//
//    // === 과거 컨트롤러 시그니처 호환용 오버로드 ===
//    @Transactional
//    public List<RagChunkEntity> registerResumeChunks(Integer userId,
//                                                     ict.project.entity.ResumeEntity saved,
//                                                     String content,
//                                                     String source) {
//        String filePath = "RESUME".equalsIgnoreCase(source) ? saved.getFilePath() : saved.getJobfilePath();
//        return registerUserChunks(userId, source, filePath, content);
//    }
//
//}


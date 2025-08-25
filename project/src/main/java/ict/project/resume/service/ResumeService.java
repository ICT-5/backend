package ict.project.resume.service;

import ict.project.resume.entity.ResumeEntity;
import ict.project.resume.entity.UserEntity;
import ict.project.resume.repository.ResumeRepository;
import ict.project.resume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    /** 파일 저장 루트 (예: /var/app/uploads) */
    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    /** 이력서 업로드 하위 폴더 */
    private static final String RESUME_DIR = "resume";
    /** 채용공고(크롤링 결과) 저장 하위 폴더 */
    private static final String JOBPOST_DIR = "jobposting";

    /**
     * 2. 사용자한테 이력서 파일 받아서 서버 경로에 저장
     * 3. 이력서 서버 경로 및 유저아이디 DB에 저장
     */
    @Transactional
    public ResumeEntity saveResume(Integer userId, MultipartFile resumeFile) throws IOException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        // 디렉토리: /{uploadRoot}/resume/{userId}/
        Path userDir = Paths.get(uploadRoot, RESUME_DIR, String.valueOf(userId)).toAbsolutePath().normalize();
        Files.createDirectories(userDir);

        String original = resumeFile.getOriginalFilename();
        String safeName = (original == null || original.isBlank()) ? "resume.pdf" : original.replaceAll("[\\\\/:*?\"<>|]", "_");
        String stamped = System.currentTimeMillis() + "_" + safeName;

        Path target = userDir.resolve(stamped);
        // 이미 존재하면 교체
        Files.copy(resumeFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        ResumeEntity resume = resumeRepository.findTopByUserOrderByUpdatedAtDesc(user)
                .orElse(ResumeEntity.builder().user(user).build());

        resume.setFileName(safeName);
        resume.setFilePath(relativizeForDb(target));
        resume.setUpdatedAt(LocalDateTime.now());

        return resumeRepository.save(resume);
    }

    /**
     * 1. 관리자가 크롤링한 데이터 1번만 RAG 하면,
     *  - (본 메서드는 사용자별 채용공고 URL을 받아 크롤링 → 파일 저장)
     * 2/3. 채용공고 서버 경로 및 유저아이디 DB 저장
     *
     * @return 업데이트된 ResumeEntity (사용자의 최신 레코드에 jobfile_* 설정)
     */
    @Transactional
    public ResumeEntity crawlJobPostingAndAttach(Integer userId, String url) throws IOException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        // 크롤링 (필요: org.jsoup:jsoup 의존성)
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; JobCrawler/1.0)")
                .timeout(15_000)
                .get();

        String title = doc.title();
        String bodyText = Jsoup.clean(doc.body().text(), Safelist.none()); // 텍스트만

        // 저장 디렉토리: /{uploadRoot}/jobposting/{userId}/
        Path userDir = Paths.get(uploadRoot, JOBPOST_DIR, String.valueOf(userId)).toAbsolutePath().normalize();
        Files.createDirectories(userDir);

        String baseName = (title == null || title.isBlank()) ? "jobposting" : title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = System.currentTimeMillis() + "_" + baseName + ".txt";
        Path out = userDir.resolve(fileName);

        Files.writeString(out, "URL: " + url + System.lineSeparator()
                        + "TITLE: " + title + System.lineSeparator()
                        + "CRAWLED_AT: " + LocalDateTime.now() + System.lineSeparator()
                        + System.lineSeparator()
                        + bodyText,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 사용자의 최신 ResumeEntity에 jobfile_* 업데이트 (없으면 새로 생성)
        ResumeEntity resume = resumeRepository.findTopByUserOrderByUpdatedAtDesc(user)
                .orElse(ResumeEntity.builder().user(user).updatedAt(LocalDateTime.now()).build());

        resume.setJobfileName(fileName);
        resume.setJobfilePath(relativizeForDb(out));
        resume.setUpdatedAt(LocalDateTime.now());

        return resumeRepository.save(resume);
    }

    /**
     * 4. 분석 버튼: 최신 이력서 레코드 반환 (컨트롤러/서비스 상위 계층에서 RAG 분석에 사용)
     */
    @Transactional(readOnly = true)
    public ResumeEntity getLatestResume(Integer userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));
        return resumeRepository.findTopByUserOrderByUpdatedAtDesc(user)
                .orElseThrow(() -> new IllegalStateException("업로드된 이력서/채용공고가 없습니다."));
    }

    /**
     * 절대경로를 DB에는 업로드 루트 기준 상대경로로 저장하면 이식성↑
     */
    private String relativizeForDb(Path absolute) {
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        try {
            Path rel = root.relativize(absolute.toAbsolutePath().normalize());
            return Paths.get(uploadRoot).resolve(rel).toString().replace("\\", "/"); // 운영 환경 경로 표시 통일
        } catch (IllegalArgumentException e) {
            // 루트 밖이면 절대경로 저장
            return absolute.toString().replace("\\", "/");
        }
    }
}

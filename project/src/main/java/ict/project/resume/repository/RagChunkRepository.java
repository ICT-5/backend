package ict.project.resume.repository;

import ict.project.resume.entity.RagChunkEntity;
import ict.project.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RagChunkRepository extends JpaRepository<RagChunkEntity, Integer> {

    List<RagChunkEntity> findByUserAndSource(UserEntity user, String source);

    Optional<RagChunkEntity> findTopByUserAndSourceOrderByCreatedAtDesc(UserEntity user, String source);

    boolean existsByUserAndSourceAndFilePathAndContent(UserEntity user, String source, String filePath, String content);
}

package ict.project.resume.repository;

import ict.project.resume.entity.ResumeEntity;
import ict.project.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<ResumeEntity, Integer> {
    List<ResumeEntity> findByUser(UserEntity user);
    Optional<ResumeEntity> findTopByUserOrderByUpdatedAtDesc(UserEntity user);
}

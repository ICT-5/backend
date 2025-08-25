package ict.project.resume.repository;

import ict.project.resume.entity.RagSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RagSettingsRepository extends JpaRepository<RagSettingsEntity, Integer> {
    Optional<RagSettingsEntity> findByName(String name);

}

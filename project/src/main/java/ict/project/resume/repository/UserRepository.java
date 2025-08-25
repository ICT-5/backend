package ict.project.resume.repository;

import ict.project.resume.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);
    Optional<UserEntity> findById(Integer id);
}

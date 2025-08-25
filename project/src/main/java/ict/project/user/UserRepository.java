package ict.project.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    //UserEntity findByEmail(String email);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);
    Optional<UserEntity> findById(Integer id);

}

// src/main/java/ict/project/security/UserDetailsServiceImpl.java
package ict.project.config;

import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username == email 로 사용
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 권한 매핑: 엔티티에 roles 가 없으면 기본 ROLE_USER 하나만 부여
        Collection<? extends GrantedAuthority> authorities = resolveAuthorities(user);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())        // ← UserEntity.getEmail()
                .password(user.getPassword())         // ← 반드시 인코딩된 해시(BCrypt 등)
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(UserEntity user) {
        // [선택] UserEntity에 roles 같은 필드가 있으면 거기서 변환:
        // return user.getRoles().stream()
        //    .map(r -> new SimpleGrantedAuthority(r.getName())) // "ROLE_USER" 형태
        //    .toList();

        // 기본값: ROLE_USER 하나만
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}

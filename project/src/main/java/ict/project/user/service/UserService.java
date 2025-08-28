package ict.project.user.service;

import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import ict.project.user.dto.LoginRequestDto;
import ict.project.user.dto.ProfileRequestDto;
import ict.project.user.dto.ProfileResponseDto;
import ict.project.user.dto.SignUpRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입
    public String signup(SignUpRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        return "회원가입 성공";
    }

    // 로그인 -> JWT 발급 (✅ userId 클레임 포함)
    public String login(LoginRequestDto request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // ✅ 변경: 이메일이 아니라 UserEntity로 생성 → userId 클레임 포함된 토큰
        return jwtUtil.generateToken(user);
    }

    public ProfileResponseDto updateProfile(Integer userId, ProfileRequestDto request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        user.setTechStack(request.getTechStack());
        user.setJobCareer(request.getJobCareer());
        user.setEducationCareer(request.getEducationCareer());
        user.setJobCategory(request.getJobCategory());
        user.setJobRole(request.getJobRole());

        UserEntity saved = userRepository.save(user);

        return ProfileResponseDto.builder()
                .username(saved.getUsername())
                .email(saved.getEmail())
                .techStack(saved.getTechStack())
                .jobCareer(saved.getJobCareer())
                .educationCareer(saved.getEducationCareer())
                .jobCategory(saved.getJobCategory())
                .jobRole(saved.getJobRole())
                .build();
    }
}

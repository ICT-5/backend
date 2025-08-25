package ict.project.user;

import ict.project.user.dto.LoginRequestDto;
import ict.project.user.dto.ProfileRequestDto;
import ict.project.user.dto.ProfileResponseDto;
import ict.project.user.dto.SignUpRequestDto;
import ict.project.user.service.JwtUtil;
import ict.project.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequestDto request) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponseDto> updateProfile(@RequestHeader("Authorization") String authHeader,
                                                            @RequestBody ProfileRequestDto request) {
        String token = authHeader.replace("Bearer ", "");
        Integer userId = jwtUtil.getUserId(token);

        ProfileResponseDto response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Integer id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

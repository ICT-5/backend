package ict.project.resumeAnalyze;

import ict.project.resumeAnalyze.dto.InputRequestDto;
import ict.project.resumeAnalyze.dto.ResumeQuestionDto;
import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import ict.project.user.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResumeAnalyzeController {

    @Autowired
    private ResumeAnalyzeService resumeAnalyzeService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/questions")
    public List<ResumeQuestionDto> getInterviewQuestions(@RequestHeader("Authorization") String authHeader,
                                                         @RequestBody InputRequestDto request) {
        String token = authHeader.replace("Bearer ", "");
        Integer userId = jwtUtil.getUserId(token);

        return resumeAnalyzeService.generateQuestions(userId,request);
    }
}

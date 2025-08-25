package ict.project.resumeAnalyze;

import ict.project.resumeAnalyze.dto.InputRequestDto;
import ict.project.resumeAnalyze.dto.ResumeQuestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResumeAnalyzeController {

    @Autowired
    private ResumeAnalyzeService resumeAnalyzeService;

    @PostMapping("/questions")
    public List<ResumeQuestionDto> getInterviewQuestions(@RequestBody InputRequestDto request) {
        return resumeAnalyzeService.generateQuestions(request);
    }
}

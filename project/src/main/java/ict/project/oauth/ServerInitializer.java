package ict.project.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.model.user.User;
import ict.project.oauth.repository.InMemoryUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServerInitializer implements ApplicationRunner {
    private final InMemoryUserRepository inMemoryUserRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        inMemoryUserRepository.save(User.builder().name("AN-SO").oauthId("1234").oauthType(OauthType.GITHUB).build());
        inMemoryUserRepository.save(User.builder().name("san2").oauthId("5678").oauthType(OauthType.GITHUB).build());

        log.info("Initialized users.");
    }
}

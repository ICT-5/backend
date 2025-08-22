package ict.project.oauth.repository;

import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.model.user.User;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class InMemoryUserRepository {
    private final Map<Integer, User> userStorage = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger();

    public User findById(int id) {
        return userStorage.get(id);
    }

    public User findByOAuthIdAndOAuthType(String oauthId, OauthType oauthType) {
        return userStorage.values().stream()
                .filter(user -> user.getOauthId().equals(oauthId) && user.getOauthType().equals(oauthType))
                .findAny()
                .orElse(null);
    }

    public User save(User user) {
        int id = idGenerator.incrementAndGet();
        user.setId(id);
        userStorage.put(id, user);
        return user;
    }

    public void deleteById(int id) {
        userStorage.remove(id);
    }

    public Map<Integer, User> findAll() {
        return new HashMap<>(userStorage);
    }


}

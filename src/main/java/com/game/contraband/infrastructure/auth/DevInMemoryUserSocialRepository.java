package com.game.contraband.infrastructure.auth;

import com.game.contraband.domain.auth.repository.UserSocialRepository;
import com.game.contraband.domain.user.User;
import com.game.contraband.domain.user.vo.Social;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile("dev")
@Repository
public class DevInMemoryUserSocialRepository implements UserSocialRepository {

    private static final Set<Long> ALLOWED_USER_IDS = Set.of(2001L, 2002L, 2003L, 2004L, 2005L, 2006L);
    private final Map<String, User> usersBySocialId = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        usersBySocialId.put(user.getSocialId(), user);
        return user;
    }

    @Override
    public Optional<User> find(Social social) {
        if (social == null || social.socialId() == null) {
            return Optional.empty();
        }
        Long parsedUserId = parseUserId(social.socialId());
        if (parsedUserId == null || !ALLOWED_USER_IDS.contains(parsedUserId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersBySocialId.computeIfAbsent(social.socialId(), ignored -> User.create(social)
                                                                                                 .updateAssignedId(parsedUserId)));
    }

    private Long parseUserId(String socialId) {
        try {
            return Long.parseLong(socialId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

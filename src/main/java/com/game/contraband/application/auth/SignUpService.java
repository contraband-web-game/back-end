package com.game.contraband.application.auth;

import com.game.contraband.domain.auth.repository.UserSocialRepository;
import com.game.contraband.domain.user.User;
import com.game.contraband.domain.user.vo.Social;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("!dev")
@Service
@RequiredArgsConstructor
class SignUpService {

    private final UserSocialRepository userSocialRepository;

    public User signUp(Social social) {
        User user = User.create(social);

        return userSocialRepository.save(user);
    }
}

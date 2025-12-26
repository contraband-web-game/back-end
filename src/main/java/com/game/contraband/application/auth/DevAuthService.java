package com.game.contraband.application.auth;

import com.game.contraband.domain.auth.repository.UserSocialRepository;
import com.game.contraband.domain.user.vo.RegistrationId;
import com.game.contraband.domain.user.vo.Social;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("dev")
@Service
public class DevAuthService {

    private final UserSocialRepository userSocialRepository;

    public DevAuthService(UserSocialRepository userSocialRepository) {
        this.userSocialRepository = userSocialRepository;
    }

    public boolean isAllowedUser(Long userId) {
        Social social = new Social(RegistrationId.KAKAO, String.valueOf(userId));
        return userSocialRepository.find(social).isPresent();
    }
}

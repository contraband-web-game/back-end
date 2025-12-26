package com.game.contraband.domain.auth.repository;

import com.game.contraband.domain.user.User;
import com.game.contraband.domain.user.vo.Social;
import java.util.Optional;

public interface UserSocialRepository {

    User save(User user);

    Optional<User> find(Social social);
}

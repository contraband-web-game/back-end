package com.game.contraband.domain.user;

import com.game.contraband.domain.common.AuditTimestamps;
import com.game.contraband.domain.user.vo.Nickname;
import com.game.contraband.domain.user.vo.RegistrationId;
import com.game.contraband.domain.user.vo.Social;
import com.game.contraband.domain.user.vo.UserId;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = "id")
public class User {

    private final UserId id;
    private final Nickname nickname;
    private final Social social;
    private final AuditTimestamps timestamps;

    public static User create(Social social) {
        return new User(UserId.EMPTY_USER_ID, Nickname.EMPTY_NICKNAME, social, AuditTimestamps.now());
    }

    public static User create(
            Long userId,
            String nickname,
            String registrationId,
            String socialId,
            LocalDateTime createAt,
            LocalDateTime updatedAt
    ) {
        RegistrationId userRegistrationId = RegistrationId.findBy(registrationId);
        Social social = new Social(userRegistrationId, socialId);

        return new User(
                UserId.create(userId),
                Nickname.create(nickname),
                social,
                AuditTimestamps.create(createAt, updatedAt)
        );
    }

    private User(UserId id, Nickname nickname, Social social, AuditTimestamps timestamps) {
        this.id = id;
        this.nickname = nickname;
        this.social = social;
        this.timestamps = timestamps;
    }

    public User updateAssignedId(Long id) {
        return new User(UserId.create(id), this.nickname, this.social, this.timestamps);
    }

    public User changeNickname(String changedNickname) {
        return new User(this.id, Nickname.create(changedNickname), this.social, this.timestamps);
    }

    public UserId id() {
        return this.id;
    }

    public Long getId() {
        return this.id.getValue();
    }

    public String getNickname() {
        return nickname.getValue();
    }

    public String getRegistrationId() {
        return social.registrationId().name();
    }

    public String getSocialId() {
        return social.socialId();
    }

    public LocalDateTime getCreatedAt() {
        return timestamps.getCreatedAt();
    }

    public LocalDateTime getUpdatedAt() {
        return timestamps.getUpdatedAt();
    }
}

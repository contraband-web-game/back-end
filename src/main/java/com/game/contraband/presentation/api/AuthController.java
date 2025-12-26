package com.game.contraband.presentation.api;

import com.game.contraband.application.auth.DevAuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DevAuthService devAuthService;

    public AuthController(DevAuthService devAuthService) {
        this.devAuthService = devAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.userId() == null) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "userId는 필수입니다."));
        }
        if (!devAuthService.isAllowedUser(request.userId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "허용되지 않은 ID입니다."));
        }
        return ResponseEntity.ok(new LoginResponse(true, "로그인 성공"));
    }

    public record LoginRequest(Long userId) { }

    public record LoginResponse(boolean success, String message) { }
}

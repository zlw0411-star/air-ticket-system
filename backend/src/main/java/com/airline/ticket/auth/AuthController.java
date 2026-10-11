package com.airline.ticket.auth;

import com.airline.ticket.common.ApiResponse;
import com.airline.ticket.user.SysUser;
import com.airline.ticket.user.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Validated @RequestBody RegisterBody body) {
        return ApiResponse.ok(authService.register(new AuthService.RegisterReq(
                body.username(), body.password(), body.realName(), body.idCard(), body.phone(), body.email())));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Validated @RequestBody LoginBody body) {
        return ApiResponse.ok(authService.login(new AuthService.LoginReq(body.username(), body.password())));
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> me(Authentication authentication) {
        if (authentication == null) {
            return ApiResponse.fail(401, "未登录");
        }
        return ApiResponse.ok(userService.currentUser());
    }

    public record RegisterBody(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String realName,
            String idCard,
            String phone,
            String email) {}

    public record LoginBody(@NotBlank String username, @NotBlank String password) {}
}

package com.airline.ticket.user;

import com.airline.ticket.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<SysUser> profile() {
        return ApiResponse.ok(userService.currentUser());
    }

    @PutMapping("/profile")
    public ApiResponse<SysUser> update(@RequestBody ProfileReq req) {
        return ApiResponse.ok(userService.updateProfile(req.realName(), req.phone(), req.email()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> password(@Validated @RequestBody PasswordReq req) {
        userService.changePassword(req.oldPassword(), req.newPassword());
        return ApiResponse.ok();
    }

    public record ProfileReq(String realName, String phone, String email) {}
    public record PasswordReq(@NotBlank String oldPassword, @NotBlank String newPassword) {}
}

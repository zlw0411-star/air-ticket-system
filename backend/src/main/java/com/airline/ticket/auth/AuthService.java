package com.airline.ticket.auth;

import com.airline.ticket.common.BusinessException;
import com.airline.ticket.security.JwtService;
import com.airline.ticket.user.SysUser;
import com.airline.ticket.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(RegisterReq req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password()) || isBlank(req.realName())) {
            throw new BusinessException("用户名、密码和姓名不能为空");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRealName(req.realName());
        user.setIdCard(req.idCard());
        user.setPhone(req.phone());
        user.setEmail(req.email());
        user.setRole("PASSENGER");
        user.setEnabled(true);
        userMapper.insert(user);
        return loginPayload(user);
    }

    public Map<String, Object> login(LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账号已被禁用");
        }
        return loginPayload(user);
    }

    private Map<String, Object> loginPayload(SysUser user) {
        return Map.of("token", jwtService.generate(user), "user", user);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record RegisterReq(String username, String password, String realName, String idCard, String phone, String email) {}
    public record LoginReq(String username, String password) {}
}

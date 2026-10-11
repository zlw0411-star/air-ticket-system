package com.airline.ticket.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoUserInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureUser("admin", "Admin123", "系统管理员", "ADMIN");
        ensureUser("user", "User123", "演示乘客", "PASSENGER");
    }

    private void ensureUser(String username, String password, String realName, String role) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setEnabled(true);
        user.setPhone(role.equals("ADMIN") ? "13800000000" : "13900000000");
        user.setEmail(username + "@demo.local");
        userMapper.insert(user);
    }
}

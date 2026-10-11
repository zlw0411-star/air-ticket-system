package com.airline.ticket.user;

import com.airline.ticket.common.BusinessException;
import com.airline.ticket.common.PageResult;
import com.airline.ticket.security.AuthUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUser currentUser() {
        AuthUser authUser = (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userMapper.selectById(authUser.getId());
    }

    public SysUser updateProfile(String realName, String phone, String email) {
        SysUser user = currentUser();
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        userMapper.updateById(user);
        return user;
    }

    public void changePassword(String oldPassword, String newPassword) {
        SysUser user = currentUser();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public PageResult<SysUser> adminList(String keyword, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public void updateStatus(Long id, Boolean enabled) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole()) && Boolean.FALSE.equals(enabled)) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setEnabled(enabled);
        userMapper.updateById(user);
    }
}

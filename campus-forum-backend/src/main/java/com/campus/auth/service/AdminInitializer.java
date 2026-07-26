package com.campus.auth.service;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时从环境变量/配置初始化管理员账号密码。
 * 不修改 V2__init_data.sql 中的默认管理员，仅在配置了 campus.admin.password 时
 * 创建或更新管理员账号，便于部署时通过环境变量注入强密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${campus.admin.student-no:admin001}")
    private String adminStudentNo;

    @Value("${campus.admin.email:}")
    private String adminEmail;

    @Value("${campus.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 未配置密码：绝不创建任何管理员账户，避免随仓库分发的默认凭据（V-C）
        if (adminPassword == null || adminPassword.isBlank()) {
            log.error("未配置 CAMPUS_ADMIN_PASSWORD：不创建任何管理员账户（避免默认凭据）");
            return;
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);
        User existing = userMapper.selectByStudentNo(adminStudentNo);

        if (existing == null) {
            User admin = new User();
            admin.setStudentNo(adminStudentNo);
            admin.setNickname("管理员");
            admin.setPassword(encodedPassword);
            admin.setEmail(adminEmail.isBlank() ? null : adminEmail);
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setLoginFail(0);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setDeleted(0);
            userMapper.insert(admin);
            log.info("已创建管理员账号: studentNo={}", adminStudentNo);
        } else if (!passwordEncoder.matches(adminPassword, existing.getPassword())) {
            // 配置密码与库中不一致，则更新密码
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.eq(User::getId, existing.getId())
                    .set(User::getPassword, encodedPassword)
                    .set(User::getUpdatedAt, LocalDateTime.now());
            userMapper.update(null, wrapper);
            log.info("已更新管理员密码: studentNo={}", adminStudentNo);
        } else {
            log.info("管理员密码已是最新，无需更新: studentNo={}", adminStudentNo);
        }
    }
}

package com.campus.common.util;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender mailSender;

    /**
     * Send verification code email asynchronously.
     *
     * @param email recipient email address
     * @param code  6-digit verification code
     */
    public void sendVerifyCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("校园论坛验证码");
            helper.setText(buildVerifyCodeHtml(code), true);
            mailSender.send(message);
            log.info("验证码邮件已发送至: {}", email);
        } catch (Exception e) {
            log.error("发送验证码邮件失败: {}", email, e);
            throw new IllegalStateException("验证码邮件发送失败", e);
        }
    }

    private String buildVerifyCodeHtml(String code) {
        return """
            <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                <h2 style="color:#333;">校园论坛验证码</h2>
                <p>您好，您正在注册校园论坛账号，验证码如下：</p>
                <div style="background:#f5f5f5;padding:15px;text-align:center;font-size:24px;font-weight:bold;letter-spacing:5px;color:#333;">
                    %s
                </div>
                <p style="color:#999;font-size:14px;">验证码 5 分钟内有效，请勿泄露给他人。</p>
            </div>
            """.formatted(code);
    }
}

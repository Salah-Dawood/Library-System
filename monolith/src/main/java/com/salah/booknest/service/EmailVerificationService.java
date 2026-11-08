package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.UsernameRequest;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_VALID_MINUTES = 10;

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final JWTUtils jwtUtils;
    private final TemplateEngine templateEngine;

    @Value("${server.root}")
    private String root;

    @Value("${ngrok.url}")
    private String ngrokUrl;

    @Autowired
    public EmailVerificationService(UserRepository userRepository,
                                    JavaMailSender mailSender,
                                    JWTUtils jwtUtils, TemplateEngine templateEngine){
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.jwtUtils = jwtUtils;
        this.templateEngine = templateEngine;
    }

    // This reads configured username to use as the sender address
    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.domain}")
    private String domain;

    @Value("${app.email.gif-url}")
    private String gifUrl;




    //SEND EMAIL METHOD used by all methods that send an email
    public void sendEmail(String email, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail + domain);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send HTML email to " + email, e);
        }
    }


    public void sendVerificationCode(User user) {
        int code = generateVerificationCode();
        String email = user.getEmailAddress();
        String subject = "BookNest - Verify Your Email Address";

        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("validMinutes", CODE_VALID_MINUTES);
        context.setVariable("Gif", gifUrl);

        String htmlContent = templateEngine.process("email-verification", context);

        user.setEmailVerificationCode(code);
        user.setEmailVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES));
        userRepository.save(user);
        sendEmail(email, subject, htmlContent);
    }

    //generate code helper method
    public int generateVerificationCode() {
        // Generates a number from 0 to 8999, then adds 1000 to ensure it's always 4 digits
        return 1000 + SECURE_RANDOM.nextInt(9000);
    }

    public boolean verifyEmail(String username, int code) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        Integer stored = user.getEmailVerificationCode();
        if (stored == null || stored != code) {
            throw new InvalidRequestException("Incorrect verification code");
        }
        LocalDateTime expiresAt = user.getEmailVerificationCodeExpiresAt();
        if (expiresAt == null || LocalDateTime.now().isAfter(expiresAt)) {
            throw new InvalidRequestException("Verification code has expired. Please request a new one");
        }
        user.setIsVerified(true);
        // The code can only be used once
        user.setEmailVerificationCode(null);
        user.setEmailVerificationCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    /**
     * Never reveals whether the username exists: an unknown username (or a mail failure) is only
     * logged, so the caller always gets the same response.
     */
    public void sendResetEmail(UsernameRequest request) {
        Optional<User> found = userRepository.findUserByUsername(request.getUsername());
        if (found.isEmpty()) {
            log.debug("Password reset requested for an unknown username");
            return;
        }
        User user = found.get();

        try {
            String token = jwtUtils.generatePasswordResetToken(request.getUsername());
            String resetUrl = ngrokUrl + "/#/reset?token=" + token;
            String subject = "BookNest - Password Reset";

            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("Gif", gifUrl);

            String htmlContent = templateEngine.process("forgot-password", context);

            sendEmail(user.getEmailAddress(), subject, htmlContent);
        } catch (RuntimeException e) {
            log.error("Could not send the password reset email", e);
        }
    }
}

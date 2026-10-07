package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.UsernameRequest;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Random;

@Service
public class EmailVerificationService {

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
        context.setVariable("Gif", gifUrl);

        String htmlContent = templateEngine.process("email-verification", context);

        user.setEmailVerificationCode(code);
        userRepository.save(user);
        sendEmail(email, subject, htmlContent);
    }

    //generate code helper method
    public int generateVerificationCode() {
        Random random = new Random();
        // Generates a number from 0 to 8999, then adds 1000 to ensure it's always 4 digits
        return 1000 + random.nextInt(9000);
    }

    public boolean verifyEmail(String username, int code) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        Integer stored = user.getEmailVerificationCode();
        if (stored == null || stored != code) {
            throw new InvalidRequestException("Incorrect verification code");
        }
        user.setIsVerified(true);
        userRepository.save(user);
        return true;
    }

    public void sendResetEmail(UsernameRequest request) {
        User user = userRepository.findUserByUsername(request.getUsername())
                .orElseThrow(() -> new InformationNotFoundException("Username " + request.getUsername() + " not found"));

        String token = jwtUtils.generatePasswordResetToken(request.getUsername());
        String resetUrl = ngrokUrl + "/#/reset?token=" + token;
        String subject = "BookNest - Password Reset";

        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("Gif", gifUrl);

        String htmlContent = templateEngine.process("forgot-password", context);

        sendEmail(user.getEmailAddress(), subject, htmlContent);
    }
}

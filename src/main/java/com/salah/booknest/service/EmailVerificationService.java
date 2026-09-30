package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final JWTUtils jwtUtils;

    @Value("${server.root}")
    private String root;

    @Autowired
    public EmailVerificationService(UserRepository userRepository,
                                    JavaMailSender mailSender,
                                    JWTUtils jwtUtils){
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.jwtUtils = jwtUtils;
    }

    // This reads configured username to use as the sender address
    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.domain}")
    private String domain;



    //SEND EMAIL METHOD used by all methods that send an email

    public void sendEmail(String email,String subject,String text){
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail + domain);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void sendVerificationCode(User user) {
        int code = generateVerificationCode();

        String email = user.getEmailAddress();
        String subject = "BookNest - Verify Your Email Address";
        String text = "Your verification code is: " + code;

        user.setEmailVerificationCode(code);
        userRepository.save(user);
        sendEmail(email,subject,text);
    }

    //generate code helper method
    public int generateVerificationCode() {
        Random random = new Random();
        // Generates a number from 0 to 8999, then adds 1000 to ensure it's always 4 digits
        return 1000 + random.nextInt(9000);
    }

    public String verifyEmail(String username,int code){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        if (user.getEmailVerificationCode() == code){
            user.setIsVerified(true);
            userRepository.save(user);
            return "success";
        } else {
            return "no";
        }
    }

    public String sendResetEmail(String username){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        String password = generatePassword();
        String email = user.getEmailAddress();
        String subject = "Book Nest - Password Reset";
        String token = jwtUtils.generatePasswordResetToken(username);
        String text = "Ignore this email if did not initiate the password reset process\n" +
                "Click the folliwing link to reset you password:\n" +
                root + "/auth/users/passwordreset/" + token + "/" + password + "\nyour password will be set to " + password;
        sendEmail(email,subject,text);
        return "yooho";
    }

    public String generatePassword(){
        String pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(pool.length());
            password.append(pool.charAt(index));
        }

        return password.toString();
    }

}
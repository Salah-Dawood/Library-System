package com.salah.booknest;

import com.salah.booknest.controller.UserController;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoginRequest;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.MyUserDetails;
import com.salah.booknest.security.Roles;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.AuditLogService;
import com.salah.booknest.service.EmailVerificationService;
import com.salah.booknest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityAndLoginTest {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JWTUtils jwtUtils;
    private AuthenticationManager authenticationManager;
    private EmailVerificationService emailVerificationService;
    private AuditLogService auditLogService;
    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = new JWTUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "Ym9va25lc3QtdGVzdC1zZWNyZXQta2V5LWF0LWxlYXN0LTMyLWNoYXJz");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60000);
        authenticationManager = mock(AuthenticationManager.class);
        emailVerificationService = mock(EmailVerificationService.class);
        auditLogService = mock(AuditLogService.class);
        userService = new UserService(userRepository, passwordEncoder, jwtUtils,
                authenticationManager, auditLogService);
    }

    @Test
    void login_verifiedUserReturnsJwtAndAuthenticates() {
        User user = user(1L, "alice", true);
        MyUserDetails details = new MyUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, "password", details.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        var response = userController.loginUser(loginRequest("alice", "password"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginUnverifiedUserReturnsForbiddenWithoutGeneratingToken() {
        User user = user(1L, "alice", false);
        MyUserDetails details = new MyUserDetails(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, "password", details.getAuthorities()));

        var response = userController.loginUser(loginRequest("alice", "password"));

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void jwtRoundTripValidatesAndExtractsUsername() {
        String token = jwtUtils.generateJwtToken(new MyUserDetails(user(1L, "alice", true)));

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("alice", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void jwtRejectsMalformedToken() {
        assertFalse(jwtUtils.validateJwtToken("not.a.valid.jwt"));
    }

    @Test
    void rolesRecognizesLibrarianButNotMember() {
        Authentication librarian = new UsernamePasswordAuthenticationToken(
                "lib", null, List.of(new SimpleGrantedAuthority("ROLE_librarian")));
        Authentication member = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_member")));

        assertTrue(Roles.isLibrarian(librarian));
        assertFalse(Roles.isLibrarian(member));
    }

    @Test
    void loginRequestRejectsMissingUsernameAndPassword() {
        LoginRequest request = loginRequest("", "");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var violations = validator.validate(request);

            assertEquals(2, violations.size(),
                    "Blank username and password should both fail validation");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }
    }

    private static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private static User user(Long id, String username, boolean verified) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmailAddress(username + "@example.com");
        user.setPassword("encoded-password");
        user.setRole("Member");
        user.setIsActive(true);
        user.setIsVerified(verified);
        return user;
    }
}

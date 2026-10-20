package com.salah.booknest;

import com.salah.booknest.controller.AuthorController;
import com.salah.booknest.model.request.AuthorRequest;
import com.salah.booknest.service.AuthorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests actual Spring method-security enforcement on AuthorController's
 * @PreAuthorize("hasRole('librarian')") endpoint methods.
 */
@SpringJUnitConfig(ValidationAndAuthorizationTest.TestSecurityConfiguration.class)
class ValidationAndAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    @Import(AuthorController.class)
    static class TestSecurityConfiguration {
        @Bean
        AuthorService authorService() {
            return mock(AuthorService.class);
        }
    }

    @Autowired
    private AuthorController authorController;

    @Autowired
    private AuthorService authorService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void memberIsDeniedWhenCreatingAuthor() {
        authenticateAs("ROLE_member");

        assertThrows(AccessDeniedException.class,
                () -> authorController.createAuthor(new AuthorRequest()));
    }

    @Test
    void librarianCanCreateAuthor() {
        authenticateAs("ROLE_librarian");
        when(authorService.createAuthor(any(AuthorRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        ResponseEntity<?> response = authorController.createAuthor(new AuthorRequest());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(authorService).createAuthor(any(AuthorRequest.class));
    }

    private static void authenticateAs(String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "test-user", "not-used", List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

package com.salah.booknest.repository;

import com.salah.booknest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmailAddress(String emailAddress);
    boolean existsByUsername(String username);

    Optional<User> findUserByUsername(String username);
    User findUserById(Long id);


}

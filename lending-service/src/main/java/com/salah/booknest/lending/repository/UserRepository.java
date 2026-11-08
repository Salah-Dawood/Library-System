package com.salah.booknest.lending.repository;

import com.salah.booknest.lending.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmailAddress(String emailAddress);
    boolean existsByUsername(String username);

    Optional<User> findUserByUsername(String username);
    User findUserById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockByUsername(String username);


}

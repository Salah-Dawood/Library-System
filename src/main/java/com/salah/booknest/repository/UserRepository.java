package com.salah.booknest.repository;

import com.salah.booknest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmailAddress(String emailAddress);
    boolean existsByUsername(String username);

    User findUserByUsername(String username);
    User findUserById(Long id);


}

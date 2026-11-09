package com.salah.booknest.notification.repository;

import com.salah.booknest.notification.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
}

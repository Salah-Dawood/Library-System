package com.salah.booknest.repository;

import com.salah.booknest.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineRepository extends JpaRepository<Fine, Long> {
}

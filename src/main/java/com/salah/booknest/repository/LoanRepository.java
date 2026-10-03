package com.salah.booknest.repository;

import com.salah.booknest.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan,Long> {
    List<Loan> findAllByUserId(Long userId);

}

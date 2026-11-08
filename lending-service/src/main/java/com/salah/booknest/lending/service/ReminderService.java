package com.salah.booknest.lending.service;

import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.response.DueReminder;
import com.salah.booknest.lending.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final LoanRepository loanRepository;

    @Transactional(readOnly = true)
    public List<DueReminder> findLoansDueOn(LocalDate date) {
        List<Loan> loans = loanRepository.findAllByStatusAndDueDate(LoanStatus.BORROWED, date);
        List<DueReminder> reminders = new ArrayList<>();
        for (Loan loan : loans) {
            reminders.add(new DueReminder(loan.getUser().getUsername(), loan.getUser().getEmailAddress(),
                    loan.getBook().getTitle(), loan.getDueDate()));
        }
        return reminders;
    }
}

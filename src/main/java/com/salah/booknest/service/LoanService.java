package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.response.LoanResonse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public LoanService(LoanRepository loanRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public LoanResonse loanResonser(Loan loan){
        LoanResonse response = new LoanResonse();
        response.setId(loan.getId());
        response.setBookId(loan.getBook().getId());
        response.setBookTitle(loan.getBook().getTitle());
        response.setUsername(loan.getUser().getUsername());
        response.setLoanDate(loan.getLoanDate());
        response.setDueDate(loan.getDueDate());
        response.setReturnDate(loan.getReturnDate());
        return response;
    }

    public List<LoanResonse> getLoans(){
        List<Loan> loans = loanRepository.findAll();
        List<LoanResonse> loanResonses = new ArrayList<>();
        for (Loan loan:loans){
            loanResonses.add(loanResonser(loan));
        }
        return loanResonses;
    }

    public List<LoanResonse> getMyLoans(Authentication authentication){
        List<Loan> loans = loanRepository.findAllByUserId(getUserFromAuth(authentication).getId());
        List<LoanResonse> loanResonses = new ArrayList<>();
        for (Loan loan:loans){
            loanResonses.add(loanResonser(loan));
        }
        return loanResonses;
    }

    public List<LoanResonse> getByUserId(Long userId){
        List<Loan> loans = loanRepository.findAllByUserId(userId);
        List<LoanResonse> loanResonses = new ArrayList<>();
        for (Loan loan:loans){
            loanResonses.add(loanResonser(loan));
        }
        return loanResonses;
    }

    public ResponseEntity<?> createLoan(Authentication authentication, LoanRequest request){

        LocalDate today = LocalDate.now();

        Loan loan = new Loan();
        loan.setUser(getUserFromAuth(authentication));
        loan.setBook(bookRepository.findById(request.getBookId()).orElseThrow(() -> new InformationNotFoundException("Book with ID " + request.getBookId() + " not found")));
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(request.getDuration()));

        loanRepository.save(loan);
        return new ResponseEntity<>("Loan created", HttpStatus.CREATED);
    }


    //helper methods

    public User getUserFromAuth(Authentication authentication){
        String username = authentication.getName();
        System.out.println("Username: " + username);

        return userRepository.findUserByUsername(username).orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }
}

package com.salah.booknest.lending.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "loans")
@ToString(exclude = "fines")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the member who borrowed the book, and the book. Users and books belong to other services,
    // so only their ids and a copy of the name and title are kept here
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private String username;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column
    private String bookTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.BORROWED;

    @Column(name = "requested_days", nullable = false)
    private Integer requestedDays;

    @Column(name = "loan_date")
    private LocalDate loanDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    // Return review: filled in when a librarian inspects the returned copy
    @Column(name = "reviewed_by_username")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "damaged")
    private Boolean damaged;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @OneToMany(mappedBy = "loan", fetch = FetchType.LAZY)
    private List<Fine> fines;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
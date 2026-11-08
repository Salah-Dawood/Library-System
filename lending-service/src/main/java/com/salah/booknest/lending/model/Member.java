package com.salah.booknest.lending.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lending's own copy of a user (users belong to user-service). The row is created the first time
 * the user calls this service, from the token. Borrowing locks this row first, then the book.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "members")
public class Member {

    // the same id as in user-service, so it is not generated here
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;
}

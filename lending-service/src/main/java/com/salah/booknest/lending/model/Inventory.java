package com.salah.booknest.lending.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

/**
 * The copies of one book. Books belong to catalog-service, which creates this row when a book is added
 * and sends its title. A retired book keeps its row (for the loan history) but is no longer active.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "book_id", nullable = false, unique = true)
    private Long bookId;

    @Column
    private String bookTitle;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;

    @Column(nullable = false)
    private Integer totalCopies;

    @Column(nullable = false)
    private Integer availableCopies;
}

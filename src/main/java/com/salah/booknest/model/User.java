package com.salah.booknest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
@ToString(exclude = {"password", "userProfile"})
public class User {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    private String username;

    @Column
    private String emailAddress;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column
    @ColumnDefault("'Member'")
    private String role = "Member";

    @Column
    @ColumnDefault("true")
    private Boolean isActive = true;

    @Column
    @ColumnDefault("false")
    private Boolean isVerified = false;

    @Column
    private Integer emailVerificationCode;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    @OneToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY,orphanRemoval = true)
    private UserProfile userProfile;
}

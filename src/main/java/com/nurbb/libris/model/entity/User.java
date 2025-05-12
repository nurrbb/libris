package com.nurbb.libris.model.entity;

import com.nurbb.libris.model.entity.valueobject.Level;
import com.nurbb.libris.model.entity.valueobject.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Size(min = 5, message = "Minimum password length: 5 characters")
    @Column(nullable = false)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private int score = 0;

    @Enumerated(EnumType.STRING)
    private Level level =Level.NOVICE;

    @Column(nullable = false)
    private int totalBorrowedBooks = 0;

    @Column(nullable = false)
    private int totalReturnedBooks = 0;

    @Column(nullable = false)
    private int totalLateReturns = 0;

    @Column(nullable = false)
    private int streakTimelyReturns = 0;

    @Column(nullable = false)
    private int totalReadingDays = 0;

    @Column(nullable = false)
    private int totalReadPages = 0;

    @Column(nullable = false)
    private Boolean deleted = false;
}

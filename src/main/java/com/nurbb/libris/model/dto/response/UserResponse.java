package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.valueobject.Level;
import com.nurbb.libris.model.entity.valueobject.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private int score;
    private Level level;

    private int totalPagesRead;
    private int totalReadingDays;
    private int totalReturnedBooks;
    private int totalLateReturns;
    private int currentStreakTimelyReturns;

    private boolean deleted;
    private String createdAt;
    private String updatedAt;
}

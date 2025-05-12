package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.valueobject.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserStatisticsResponse {

    private String email;
    private Level level;
    private int score;

    private int totalBorrowedBooks;
    private int totalReturnedBooks;
    private int totalLateReturns;

    private int totalReadingDays;
    private int totalReadPages;
    private double avgPagesPerDay;
    private double avgReturnDuration;

}

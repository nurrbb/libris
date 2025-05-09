package com.nurbb.libris.model.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryStatisticsResponse {

    private long totalBooks;
    private long totalUsers;
    private long totalBorrows;
    private long borrowedBooks;
    private long availableBooks;
    private long overdueBooks;
    private double averageReturnDays;

    private List<SimpleCount> mostBorrowedBooks;
    private List<SimpleCount> topGenres;

    private String textReport;
}

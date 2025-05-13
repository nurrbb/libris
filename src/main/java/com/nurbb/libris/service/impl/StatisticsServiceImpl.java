package com.nurbb.libris.service.impl;

import com.nurbb.libris.model.dto.response.LibraryStatisticsResponse;
import com.nurbb.libris.model.dto.response.SimpleCount;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final BookRepository bookRepository;
    private final BorrowRepository borrowRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "libraryStatistics")
    @Override
    public LibraryStatisticsResponse getLibraryStatistics() {

        log.info("Generating full library statistics...");

        long totalBooks = bookRepository.count();
        long totalUsers = userRepository.count();
        long totalBorrows = borrowRepository.count();

        long borrowedBooks = borrowRepository.findAll().stream()
                .filter(borrow -> !borrow.getReturned())
                .count();

        long availableBooks = bookRepository.findAll().stream()
                .mapToLong(Book::getCount)
                .sum();

        long overdueBooks = borrowRepository.findByReturnedFalseAndDueDateBefore(LocalDate.now()).size();

        List<Borrow> returned = borrowRepository.findAll().stream()
                .filter(Borrow::getReturned)
                .filter(b -> b.getReturnDate() != null && b.getBorrowDate() != null)
                .toList();

        double avgReturnDays = returned.stream()
                .mapToLong(b -> Duration.between(
                        b.getBorrowDate().atStartOfDay(),
                        b.getReturnDate().atStartOfDay()).toDays())
                .average().orElse(0);

        Map<String, Long> borrowCountByBook = borrowRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getBook().getTitle(),
                        Collectors.counting()));

        List<SimpleCount> mostBorrowedBooks = borrowCountByBook.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new SimpleCount(e.getKey(), e.getValue()))
                .toList();

        Map<String, Long> genreCounts = bookRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        book -> book.getGenre().name(),
                        Collectors.counting()));

        List<SimpleCount> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new SimpleCount(e.getKey(), e.getValue()))
                .toList();

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("LIBRARY STATISTICS REPORT\n");
        reportBuilder.append("----------------------------\n");
        reportBuilder.append("Total Books        : ").append(totalBooks).append("\n");
        reportBuilder.append("Available Books    : ").append(availableBooks).append("\n");
        reportBuilder.append("Borrowed Books     : ").append(borrowedBooks).append("\n");
        reportBuilder.append("Overdue Books      : ").append(overdueBooks).append("\n");
        reportBuilder.append("Total Users        : ").append(totalUsers).append("\n");
        reportBuilder.append("Total Borrows      : ").append(totalBorrows).append("\n");
        reportBuilder.append("Avg Return (days)  : ").append(String.format("%.2f", avgReturnDays)).append("\n");
        reportBuilder.append("Last Updated       : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        reportBuilder.append("Top Genres:\n");
        topGenres.forEach(g -> reportBuilder.append(" - ").append(g.getName()).append(" (").append(g.getCount()).append(")\n"));

        reportBuilder.append("\nMost Borrowed Books:\n");
        mostBorrowedBooks.forEach(b -> reportBuilder.append(" - ").append(b.getName()).append(" (").append(b.getCount()).append(")\n"));

        String textReport = reportBuilder.toString();

        log.debug("Library statistics generated: totalBooks={}, totalUsers={}, overdueBooks={}",
                totalBooks, totalUsers, overdueBooks);

        return LibraryStatisticsResponse.builder()
                .totalBooks(totalBooks)
                .availableBooks(availableBooks)
                .borrowedBooks(borrowedBooks)
                .overdueBooks(overdueBooks)
                .totalUsers(totalUsers)
                .totalBorrows(totalBorrows)
                .averageReturnDays(avgReturnDays)
                .mostBorrowedBooks(mostBorrowedBooks)
                .topGenres(topGenres)
                .textReport(textReport)
                .build();
    }


    @Cacheable(value = "overdueStats")
    @Override
    public Map<String, Object> getOverdueBookStatistics() {
        log.info("Generating detailed overdue borrow report...");

        List<Borrow> overdueBorrows = borrowRepository.findByReturnedFalseAndDueDateBefore(LocalDate.now());
        long totalOverdue = overdueBorrows.size();

        Map<String, Long> overdueByUser = overdueBorrows.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getEmail(), Collectors.counting()));

        Map<String, Long> overdueByBook = overdueBorrows.stream()
                .collect(Collectors.groupingBy(b -> b.getBook().getTitle(), Collectors.counting()));

        long totalBorrows = borrowRepository.count();
        double overdueRatio = totalBorrows > 0 ? (double) totalOverdue / totalBorrows : 0;

        List<Map<String, Object>> detailedOverdues = overdueBorrows.stream().map(b -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("user", b.getUser().getEmail());
            entry.put("book", b.getBook().getTitle());
            entry.put("borrowDate", b.getBorrowDate());
            entry.put("dueDate", b.getDueDate());
            entry.put("daysOverdue", Duration.between(b.getDueDate().atStartOfDay(), LocalDate.now().atStartOfDay()).toDays());
            return entry;
        }).toList();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalBorrows", totalBorrows);
        report.put("overdueBorrows", totalOverdue);
        report.put("overdueRatio", BigDecimal.valueOf(overdueRatio).setScale(2, RoundingMode.HALF_UP));
        report.put("overdueCountByUser", overdueByUser);
        report.put("overdueCountByBook", overdueByBook);
        report.put("detailedOverdueEntries", detailedOverdues);

        log.debug("Detailed overdue report generated: {} overdues", totalOverdue);
        return report;
    }

}

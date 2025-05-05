package com.nurbb.libris.service;

import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BorrowResponse;

import java.util.List;
import java.util.UUID;

public interface BorrowService {

    BorrowResponse borrowBook(BorrowRequest request);

    BorrowResponse returnBook(UUID borrowId);

    List<BorrowResponse> getBorrowHistoryByUser(UUID userId);

    List<BorrowResponse> getAllBorrows();

    List<BorrowResponse> getOverdueBorrows();
}

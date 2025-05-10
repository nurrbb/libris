package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
@Tag(name = "Borrow Management", description = "Endpoints for borrowing and returning books")
public class BorrowController {

    private final BorrowService borrowService;

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    @Operation(summary = "Borrow a book", responses = {
            @ApiResponse(responseCode = "200", description = "Book successfully borrowed",
                    content = @Content(schema = @Schema(implementation = BorrowResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or book not available")
    })
    public ResponseEntity<BorrowResponse> borrowBook(@RequestBody BorrowRequest request) {
        return ResponseEntity.ok(borrowService.borrowBook(request));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PutMapping("/return/{borrowId}")
    @Operation(summary = "Return a borrowed book", responses = {
            @ApiResponse(responseCode = "200", description = "Book successfully returned",
                    content = @Content(schema = @Schema(implementation = BorrowResponse.class))),
            @ApiResponse(responseCode = "404", description = "Borrow record not found")
    })
    public ResponseEntity<BorrowResponse> returnBook(@PathVariable UUID borrowId) {
        return ResponseEntity.ok(borrowService.returnBook(borrowId));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get borrowing history for a user", responses = {
            @ApiResponse(responseCode = "200", description = "User's borrowing history retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
    })
    public ResponseEntity<List<BorrowResponse>> getUserBorrowHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(borrowService.getBorrowHistoryByUser(userId));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping
    @Operation(summary = "Get all borrow records", responses = {
            @ApiResponse(responseCode = "200", description = "All borrow records retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
    })
    public ResponseEntity<List<BorrowResponse>> getAllBorrows() {
        return ResponseEntity.ok(borrowService.getAllBorrows());
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue borrow records", responses = {
            @ApiResponse(responseCode = "200", description = "Overdue borrow records retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
    })
    public ResponseEntity<List<BorrowResponse>> getOverdueBorrows() {
        return ResponseEntity.ok(borrowService.getOverdueBorrows());
    }
}

package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Borrow Management", description = "Endpoints for borrowing and returning books")
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Borrow a book",
            description = "Allows a librarian to register a book as borrowed by a user.",
            requestBody = @RequestBody(
                    description = "Borrow request data including book ID, user email, borrow and due dates",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BorrowRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book successfully borrowed",
                            content = @Content(schema = @Schema(implementation = BorrowResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or book not available")
            }
    )
    public ResponseEntity<BorrowResponse> borrowBook(@Valid @org.springframework.web.bind.annotation.RequestBody BorrowRequest request) {
        return ResponseEntity.ok(borrowService.borrowBook(request));
    }

    @PutMapping("/return/{borrowId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    @Operation(
            summary = "Return a borrowed book",
            description = "Marks a borrowed book as returned.",
            parameters = {
                    @Parameter(name = "borrowId", description = "UUID of the borrow record", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book successfully returned",
                            content = @Content(schema = @Schema(implementation = BorrowResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Borrow record not found")
            }
    )
    public ResponseEntity<BorrowResponse> returnBook(@PathVariable UUID borrowId) {
        return ResponseEntity.ok(borrowService.returnBook(borrowId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Get borrowing history for a user",
            description = "Retrieves all borrowing records associated with a specific user.",
            parameters = {
                    @Parameter(name = "userId", description = "UUID of the user", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User's borrowing history retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
            }
    )
    public ResponseEntity<List<BorrowResponse>> getUserBorrowHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(borrowService.getBorrowHistoryByUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    @Operation(
            summary = "Get all borrow records",
            description = "Returns all borrow transactions for librarians. Patrons will only see their own borrow records.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All borrow records retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
            }
    )
    public ResponseEntity<List<BorrowResponse>> getAllBorrows() {
        return ResponseEntity.ok(borrowService.getAllBorrows());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Get overdue borrow records",
            description = "Retrieves all borrow records where books are overdue.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Overdue borrow records retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BorrowResponse.class))))
            }
    )
    public ResponseEntity<List<BorrowResponse>> getOverdueBorrows() {
        return ResponseEntity.ok(borrowService.getOverdueBorrows());
    }
}

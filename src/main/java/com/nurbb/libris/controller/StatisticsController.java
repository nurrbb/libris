package com.nurbb.libris.controller;

import com.nurbb.libris.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Library Statistics", description = "Endpoints for viewing library-wide statistics and reports")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping(value = "/text-report", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Export library statistics in formatted report", description = "Returns a detailed and structured summary of core library metrics such as book availability, borrowing trends, overdue data, and genre distribution — suitable for reports and administrative insights")
    public ResponseEntity<String> getTextReport() {
        return ResponseEntity.ok(statisticsService.getLibraryStatistics().getTextReport());
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/overdue")
    @Operation(
            summary = "Retrieve overdue borrowing statistics",
            description = "Returns numerical statistics regarding overdue book returns, including total borrow attempts, the count of overdue books, and the calculated overdue ratio. Useful for performance and compliance tracking.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Overdue statistics successfully retrieved.",
                            content = @Content(schema = @Schema(implementation = Map.class))
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> getOverdueStatistics() {
        return ResponseEntity.ok(statisticsService.getOverdueBookStatistics());
    }
}

package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.response.LibraryStatisticsResponse;
import com.nurbb.libris.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Library Statistics", description = "Endpoints for viewing library-wide statistics and reports")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @Operation(summary = "Get overall library statistics", description = "Returns book, user, borrow, and genre stats with text report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics fetched successfully",
                            content = @Content(schema = @Schema(implementation = LibraryStatisticsResponse.class)))
            })
    public ResponseEntity<LibraryStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getLibraryStatistics());
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue book statistics", description = "Returns total borrow count, overdue count, and overdue ratio",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Overdue statistics retrieved",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            })
    public ResponseEntity<Map<String, Object>> getOverdueStatistics() {
        return ResponseEntity.ok(statisticsService.getOverdueBookStatistics());
    }
}

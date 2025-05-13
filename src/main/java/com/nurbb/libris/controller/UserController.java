package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.dto.response.UserStatisticsResponse;
import com.nurbb.libris.service.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user with role and personal details.",
            requestBody = @RequestBody(
                    description = "User registration data",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    public ResponseEntity<UserResponse> registerUser(
            @Valid @org.springframework.web.bind.annotation.RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user's details by ID. Patrons can only access their own data.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the user to retrieve", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse user = userService.getUserById(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLibrarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

        if (!isLibrarian && !user.getEmail().equals(currentEmail)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    @Operation(
            summary = "Get all users",
            description = "Retrieves all users if librarian. Patrons only receive their own user data.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
            }
    )
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Update user information",
            description = "Updates a user's information by ID. Only librarians can perform this action.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the user to update", required = true)
            },
            requestBody = @RequestBody(
                    description = "Updated user data",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(
            summary = "Delete user by ID",
            description = "Deletes a user from the system by their ID.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the user to delete", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted successfully",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    @Operation(
            summary = "Get user reading statistics",
            description = "Retrieves reading statistics for a specific user. Patrons can only view their own stats.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the user", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics retrieved",
                            content = @Content(schema = @Schema(implementation = UserStatisticsResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<UserStatisticsResponse> getUserStats(@PathVariable UUID id) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse user = userService.getUserById(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLibrarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

        if (!isLibrarian && !user.getEmail().equals(currentEmail)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(userService.getUserStatistics(id));
    }
}

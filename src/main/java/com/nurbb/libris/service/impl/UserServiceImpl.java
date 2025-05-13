package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.repository.BorrowRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.dto.response.UserStatisticsResponse;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.entity.valueobject.Role;
import com.nurbb.libris.model.mapper.UserMapper;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.service.UserService;
import com.nurbb.libris.util.LevelUtils;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BorrowRepository  borrowRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user. Automatically assigns role based on authentication status.
     * Password is encoded. Prevents duplicate email registration.
     */

    @Override
    @Transactional
    public UserResponse registerUser(UserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. Email '{}' already in use.", request.getEmail());
            throw new InvalidRequestException("Email already registered.");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidRequestException("Password cannot be empty.");
        }

        // Determine if user is authenticated (e.g., librarian creating another user)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal().equals("anonymousUser"));

        if (!isAuthenticated) {

            request.setRole(Role.PATRON);

        } else {
            // Only librarians can assign LIBRARIAN role
            boolean isLibrarian = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

            if (!isLibrarian && request.getRole() == Role.LIBRARIAN) {
                throw new AccessDeniedException("Only librarians can create librarian accounts.");
            }

            if (!isLibrarian) {
                request.setRole(Role.PATRON);
            }

            if (isLibrarian && request.getRole() == null) {
                throw new InvalidRequestException("Librarians must specify a role.");
            }
        }

        // Encode password and assign defaults
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setScore(0);
        user.setLevel(com.nurbb.libris.util.LevelUtils.determineLevel(0));

        User saved = userRepository.save(user);
        log.info("User '{}' registered with role '{}'", request.getEmail(), request.getRole());
        return userMapper.toResponse(saved);
    }

    @Cacheable(value = "userById", key = "#id")
    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();

        boolean isLibrarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

        if (isLibrarian) {
            return userRepository.findAll().stream()
                    .map(userMapper::toResponse)
                    .toList();
        }

        return userRepository.findByEmail(currentEmail)
                .map(userMapper::toResponse)
                .map(List::of)
                .orElse(List.of());
    }


    @CacheEvict(value = "userById", key = "#id")
    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        boolean updated = false;

        if (!existing.getFullName().equals(request.getFullName())) {
            existing.setFullName(request.getFullName());
            updated = true;
        }

        if (!existing.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new InvalidRequestException("Email already registered by another user.");
            }
            existing.setEmail(request.getEmail());
            updated = true;
        }

        if (!existing.getPhone().equals(request.getPhone())) {
            existing.setPhone(request.getPhone());
            updated = true;
        }

        if (!existing.getRole().equals(request.getRole())) {
            existing.setRole(request.getRole());
            updated = true;
        }

        if (!passwordEncoder.matches(request.getPassword(), existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            updated = true;
        }

        if (!updated) {
            throw new InvalidRequestException("No changes detected. User data is already up-to-date.");
        }

        log.info("User with ID '{}' updated. New email: '{}', role: {}", id, existing.getEmail(), existing.getRole());
        return userMapper.toResponse(userRepository.save(existing));
    }

    @Override
    public UserStatisticsResponse getUserStatistics(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        double avgPagesPerDay = user.getTotalReadingDays() > 0
                ? (double) user.getTotalReadPages() / user.getTotalReadingDays() : 0;

        double avgReturnDuration = user.getTotalReturnedBooks() > 0
                ? (double) user.getTotalReadingDays() / user.getTotalReturnedBooks() : 0;

        log.debug("Fetched statistics for user '{}'", user.getEmail());

        return UserStatisticsResponse.builder()
                .email(user.getEmail())
                .level(user.getLevel())
                .score(user.getScore())
                .totalBorrowedBooks(user.getTotalBorrowedBooks())
                .totalReturnedBooks(user.getTotalReturnedBooks())
                .totalLateReturns(user.getTotalLateReturns())
                .totalReadingDays(user.getTotalReadingDays())
                .totalReadPages(user.getTotalReadPages())
                .avgPagesPerDay(Math.round(avgPagesPerDay * 100.0) / 100.0)
                .avgReturnDuration(Math.round(avgReturnDuration * 100.0) / 100.0)
                .build();
    }

    /**
     * Deletes a user by ID only if they have no active borrows.
     * Evicts cache.
     */

    @CacheEvict(value = "userById", key = "#id")
    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        boolean hasActiveBorrows = borrowRepository.existsByUserAndReturnedFalse(user);

        if (hasActiveBorrows) {
            throw new InvalidRequestException("This user still has borrowed books. Please return them first.");
        }

        userRepository.delete(user);
        log.info("User with ID '{}' and email '{}' deleted.", user.getId(), user.getEmail());

    }

    /**
     * Initializes a default admin user if not already present in the database.
     */

    @PostConstruct
    public void initAdminUser() {
        String adminEmail = "admin@libris.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.LIBRARIAN);
            admin.setPhone(null);
            admin.setScore(0);
            admin.setLevel(LevelUtils.determineLevel(0));

            userRepository.save(admin);
            log.info("Default admin user created: {}", adminEmail);
        } else {
            log.info("Admin user already exists: {}", adminEmail);
        }
    }
}

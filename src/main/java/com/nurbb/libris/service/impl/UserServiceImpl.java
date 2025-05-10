package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.dto.response.UserStatisticsResponse;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.mapper.UserMapper;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse registerUser(UserRequest request) {
        validateUserInput(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. Email '{}' already in use.", request.getEmail());
            throw new InvalidRequestException("Email already registered.");
        }

        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        log.info("User '{}' registered with role '{}'", request.getEmail(), request.getRole());
        return userMapper.toResponse(saved);
    }


    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        existing.setFullName(request.getFullName());
        existing.setEmail(request.getEmail());
        existing.setRole(request.getRole());
        existing.setPhone(request.getPhone());
        log.info("User with ID '{}' updated. New email: '{}', role: {}", id, request.getEmail(), request.getRole());
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

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        log.info("User with ID '{}' and email '{}' deleted.", user.getId(), user.getEmail());
        userRepository.delete(user);
    }

    private void validateUserInput(UserRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new InvalidRequestException("Full name cannot be empty");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new InvalidRequestException("Email cannot be empty");
        }
        if (request.getRole() == null) {
            throw new InvalidRequestException("User role must be specified");
        }
    }

}

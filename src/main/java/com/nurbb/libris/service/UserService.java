package com.nurbb.libris.service;

import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.dto.response.UserStatisticsResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse registerUser(UserRequest request);

    UserResponse getUserById(UUID id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID id, UserRequest request);

    UserStatisticsResponse getUserStatistics(UUID userId);

    void deleteUser(UUID id);
}

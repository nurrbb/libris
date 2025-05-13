package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.dto.response.UserStatisticsResponse;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.entity.valueobject.Level;
import com.nurbb.libris.model.entity.valueobject.Role;
import com.nurbb.libris.model.mapper.UserMapper;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;



import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;
    @Mock private BorrowRepository borrowRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_shouldCreatePatron_whenUnauthenticated() {
        UserRequest request = new UserRequest("Name", "email@mail.com", "1234", null, null);
        User user = new User();
        user.setEmail("email@mail.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals(Role.PATRON, request.getRole());
    }

    @Test
    void registerUser_shouldDenyLibrarianCreation_whenNotLibrarian() {
        UserRequest request = new UserRequest("Name", "email@mail.com", "1234", null, Role.LIBRARIAN);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user");
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        assertThrows(AccessDeniedException.class, () -> userService.registerUser(request));
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserResponse());

        UserResponse result = userService.getUserById(userId);

        assertNotNull(result);
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserById(userId));
    }


    @Test
    void getAllUsers_shouldReturnAllMappedUsers() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@user.com");
        when(auth.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
        );

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        List<User> users = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(2, result.size());
    }



    @Test
    void updateUser_shouldUpdateAndReturnUser() {
        UserRequest request = new UserRequest("Updated", "new@mail.com", "pass", "123", Role.PATRON);

        User user = new User();
        user.setId(userId);
        user.setEmail("old@mail.com"); // null olmasın
        user.setFullName("Old Name");
        user.setPhone("555-000-0000");
        user.setRole(Role.PATRON);
        user.setPassword("password123");


        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponse());

        UserResponse response = userService.updateUser(userId, request);

        assertNotNull(response);
        verify(userRepository).save(user);
    }

    @Test
    void getUserStatistics_shouldCalculateCorrectly() {
        User user = new User();
        user.setEmail("mail");
        user.setScore(100);
        user.setLevel(Level.BOOKWORM);
        user.setTotalBorrowedBooks(10);
        user.setTotalReturnedBooks(5);
        user.setTotalLateReturns(2);
        user.setTotalReadingDays(10);
        user.setTotalReadPages(100);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserStatisticsResponse stats = userService.getUserStatistics(userId);

        assertEquals(10.0, stats.getAvgPagesPerDay());
        assertEquals(2.0, stats.getAvgReturnDuration());
    }

    @Test
    void deleteUser_shouldThrow_whenUserHasActiveBorrow() {
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserAndReturnedFalse(user)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void deleteUser_shouldDelete_whenNoActiveBorrow() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserAndReturnedFalse(user)).thenReturn(false);

        userService.deleteUser(userId);

        verify(userRepository).delete(user);
    }
}
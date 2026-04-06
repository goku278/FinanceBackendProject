package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.exceptions.BadRequestException;
import com.example.finance_app.finance_app.exceptions.ResourceNotFoundException;
import com.example.finance_app.finance_app.models.UserRole;
import com.example.finance_app.finance_app.models.dto.UserDTO;
import com.example.finance_app.finance_app.repository.UserRepository;
import com.example.finance_app.finance_app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO.UserCreateRequest createRequest;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .active(true)
                .roles(Set.of(UserRole.ROLE_VIEWER))
                .build();

        createRequest = UserDTO.UserCreateRequest.builder()
                .username("newuser")
                .password("rawPassword")
                .email("new@example.com")
                .roles(Set.of(UserRole.ROLE_ANALYST))
                .build();
    }

    // ========== createUser ==========
    @Test
    void createUser_Success() {
        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        User created = userService.createUser(createRequest);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("newuser", created.getUsername());
        assertEquals("encodedPassword", created.getPassword());
        assertEquals("new@example.com", created.getEmail());
        assertTrue(created.isActive());
        assertEquals(Set.of(UserRole.ROLE_ANALYST), created.getRoles());

        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsBadRequestException() {
        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.createUser(createRequest));
        assertEquals("Username already exists", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    // ========== updateUserRoles ==========
    @Test
    void updateUserRoles_Success() {
        Set<UserRole> newRoles = Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_ANALYST);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateUserRoles(userId, newRoles);

        assertEquals(newRoles, updated.getRoles());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserRoles_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserRoles(userId, Set.of(UserRole.ROLE_VIEWER)));
        verify(userRepository, never()).save(any());
    }

    // ========== updateUserStatus ==========
    @Test
    void updateUserStatus_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateUserStatus(userId, false);

        assertFalse(updated.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserStatus_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserStatus(userId, false));
        verify(userRepository, never()).save(any());
    }

    // ========== getUserById ==========
    @Test
    void getUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        User found = userService.getUserById(userId);

        assertEquals(testUser, found);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(userId));
    }

    // ========== getAllUsers ==========
    @Test
    void getAllUsers_ReturnsListOfUserResponses() {
        User anotherUser = User.builder()
                .id(2L)
                .username("another")
                .email("another@example.com")
                .active(true)
                .roles(Set.of(UserRole.ROLE_VIEWER))
                .build();
        when(userRepository.findAll()).thenReturn(List.of(testUser, anotherUser));

        List<UserDTO.UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());
        UserDTO.UserResponse first = responses.get(0);
        assertEquals(testUser.getId(), first.getId());
        assertEquals(testUser.getUsername(), first.getUsername());
        assertEquals(testUser.getEmail(), first.getEmail());
        assertEquals(testUser.isActive(), first.isActive());
        assertEquals(testUser.getRoles(), first.getRoles());

        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_EmptyList_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDTO.UserResponse> responses = userService.getAllUsers();

        assertTrue(responses.isEmpty());
        verify(userRepository).findAll();
    }
}
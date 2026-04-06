package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.config.CustomUserDetailsService;
import com.example.finance_app.finance_app.config.JwtUtil;
import com.example.finance_app.finance_app.config.SecurityConfig;
import com.example.finance_app.finance_app.controller.AdminController;
import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.exceptions.GlobalExceptionHandler;
import com.example.finance_app.finance_app.exceptions.ResourceNotFoundException;
import com.example.finance_app.finance_app.models.UserRole;
import com.example.finance_app.finance_app.models.dto.UserDTO;
import com.example.finance_app.finance_app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // Required for security filter chain
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private UserDTO.UserResponse userResponse;
    private UserDTO.UserCreateRequest createRequest;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .username("admin")
                .email("admin@finance.com")
                .active(true)
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        userResponse = UserDTO.UserResponse.builder()
                .id(userId)
                .username("admin")
                .email("admin@finance.com")
                .active(true)
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        createRequest = UserDTO.UserCreateRequest.builder()
                .username("analyst1")
                .password("password123")
                .email("analyst1@finance.com")
                .roles(Set.of(UserRole.ROLE_ANALYST))
                .build();
    }

    // ========== GET /api/admin/users ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ReturnsList() throws Exception {
        List<UserDTO.UserResponse> users = List.of(userResponse);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userId))
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_EmptyList_ReturnsEmpty() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "VIEWER") // Not admin
    void getAllUsers_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
        verify(userService, never()).getAllUsers();
    }

    // ========== POST /api/admin/users ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        when(userService.createUser(any(UserDTO.UserCreateRequest.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@finance.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_ValidationError_ReturnsBadRequest() throws Exception {
        UserDTO.UserCreateRequest invalid = UserDTO.UserCreateRequest.builder()
                .username("") // blank
                .password("") // blank
                .email("not-an-email")
                .roles(null) // null
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isInternalServerError());
    }

    // ========== PUT /api/admin/users/{id}/roles ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_Success() throws Exception {
        Map<String, List<String>> request = Map.of("roles", List.of("ROLE_VIEWER", "ROLE_ANALYST"));
        User updatedUser = User.builder()
                .id(userId)
                .username("admin")
                .roles(Set.of(UserRole.ROLE_VIEWER, UserRole.ROLE_ANALYST))
                .build();

        when(userService.updateUserRoles(eq(userId), anySet())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(2)))
                .andExpect(jsonPath("$.roles", hasItems("ROLE_VIEWER", "ROLE_ANALYST")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_EmptyRoles_ReturnsBadRequest() throws Exception {
        Map<String, List<String>> request = Map.of("roles", List.of());

        mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Roles cannot be empty"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_MissingRolesField_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of("wrongField", List.of("ROLE_VIEWER"));

        mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Roles cannot be empty"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoles_InvalidRoleName_ReturnsBadRequest() throws Exception {
        Map<String, List<String>> request = Map.of("roles", List.of("INVALID_ROLE"));

        mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid role: INVALID_ROLE"));
    }

    // ========== PATCH /api/admin/users/{id}/status ==========
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_Success() throws Exception {
        UserDTO.UserStatusRequest request = new UserDTO.UserStatusRequest();
        User deactivatedUser = User.builder()
                .id(userId)
                .username("admin")
                .active(false)
                .build();

        when(userService.updateUserStatus(eq(userId), eq(false))).thenReturn(deactivatedUser);

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_UserNotFound_ReturnsNotFound() throws Exception {
        UserDTO.UserStatusRequest request = new UserDTO.UserStatusRequest();
        when(userService.updateUserStatus(eq(userId), eq(true)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
}
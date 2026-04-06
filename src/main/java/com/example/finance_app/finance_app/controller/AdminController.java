package com.example.finance_app.finance_app.controller;

import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.exceptions.BadRequestException;
import com.example.finance_app.finance_app.models.UserRole;
import com.example.finance_app.finance_app.models.dto.UserDTO;
import com.example.finance_app.finance_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO.UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserDTO.UserResponse> createUser(@Valid @RequestBody UserDTO.UserCreateRequest request) {
        User user = userService.createUser(request);
        return new ResponseEntity<>(toResponse(user), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserDTO.UserResponse> updateRoles(@PathVariable Long id,
                                                            @Valid @RequestBody Map<String, List<String>> request) {
        List<String> roleNames = request.get("roles");

        if (roleNames != null || !roleNames.isEmpty()) {
            Set<UserRole> roles = roleNames.stream()
                    .map(roleName -> {
                        try {
                            return UserRole.valueOf(roleName);
                        } catch (IllegalArgumentException e) {
                            throw new BadRequestException("Invalid role: " + roleName);
                        }
                    })
                    .collect(Collectors.toSet());

            User user = userService.updateUserRoles(id, roles);
            return ResponseEntity.ok(toResponse(user));
        }
        else {
            User user = userService.updateUserRoles(id, new HashSet<>());
            return ResponseEntity.ok(toResponse(user));
        }

    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO.UserResponse> updateStatus(@PathVariable Long id,
                                                             @RequestBody UserDTO.UserStatusRequest request) {
        User user = userService.updateUserStatus(id, request.isActive());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserDTO.UserResponse toResponse(User user) {
        return UserDTO.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .active(user.isActive())
                .roles(user.getRoles())
                .build();
    }
}
package com.example.finance_app.finance_app.models.dto;

import com.example.finance_app.finance_app.models.UserRole;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
public class UserDTO {

    private String name;
    private String email;

    @Data
    @Builder
    public static class UserCreateRequest {
        private String username;
        private String password;
        private String email;
        private Set<UserRole> roles;
    }

    @Data
    @Builder
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private boolean active;
        private Set<UserRole> roles;
    }

    @Data
    @Builder
    public static class UserUpdateRoleRequest {
        private Set<UserRole> roles;
    }

    // ✅ FIXED: Add this
   /* @Data
//    @Builder
//    @NoArgsConstructor
    public static class UserStatusRequest {
        private boolean active;
    }*/

    public static class UserStatusRequest {
        private boolean active;

        // ✅ No-arg for Jackson
        public UserStatusRequest() {}

        // ✅ Setter for Jackson
        public void setActive(boolean active) {
            this.active = active;
        }

        // ✅ Getter
        public boolean isActive() {
            return active;
        }
    }
}
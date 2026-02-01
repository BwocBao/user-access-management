package com.r2s.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String username;
    private String role;
    private String email;
    private String fullName;
}

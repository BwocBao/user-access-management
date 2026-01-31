package com.r2s.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String username;
    private String Role;
    private String email;
    private String fullName;
}

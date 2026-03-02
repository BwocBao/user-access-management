package com.r2s.user.dto;

import com.r2s.core.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    private String username;
    private String password;
    private Role role;
    private String email;
    private String fullName;
}

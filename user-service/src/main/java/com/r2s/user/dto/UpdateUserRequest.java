package com.r2s.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String fullName;
    private String email;
}

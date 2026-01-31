package com.r2s.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String username;

//    @Column(nullable = false)
//    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String email;

    private String fullName;
}

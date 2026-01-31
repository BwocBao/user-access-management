package com.r2s.core.security;


import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username){
        User user= userRepository.findByUsername(username).
                orElseThrow(()->new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );

        //Nếu nhiều role
//        List<GrantedAuthority> authorities = user.getRoles()
//                .stream()
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
//                .toList();
//
//        return new org.springframework.security.core.userdetails.User(
//                user.getUsername(),
//                user.getPassword(),
//                authorities
//        );

    }

//    org.springframework.security.core.userdetails.User là class cài sẵn
//    của Spring Security implement UserDetails.
//    Username: user.getUsername()
//    Password: user.getPassword()
//    Authorities: list quyền/role của user (ROLE_USER, ROLE_ADMIN, …)
//Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())):
//    Spring Security quy ước prefix "ROLE_" cho role.
//    SimpleGrantedAuthority là object lưu role/authority.
//    singletonList vì ở ví dụ này user chỉ có 1 role.
//    Giả sử:
//    User user = new User();
//    user.setUsername("alice");
//    user.setPassword("$2a$10$hashedpassword...");
//    user.setRole("USER");
//    → Spring Security sẽ có UserDetails:
//
//    username = alice
//    password = $2a$10$hashedpassword...
//    authorities = [ROLE_USER]
//    Khi JWT filter hoặc login filter chạy, Spring Security dùng UserDetails này để:
//    So sánh password
//    Gán quyền cho request
//    Xác thực endpoint theo role
}

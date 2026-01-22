package com.r2s.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final SecretKey accessKey;
    private final Long accessExpMinutes;

    public JwtUtil(
            @Value("${jwt.access-key}") String accessKeyBase64,
            @Value("${jwt.access-exp-minutes:15}") long accessExpMinutes) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessKeyBase64));
        this.accessExpMinutes = accessExpMinutes;
    }

    public String generateToken(String username,String role) {
        Date now = new Date();
        Date expiration = Date.from(
                Instant.now().plus(accessExpMinutes, ChronoUnit.MINUTES)
        );

        return Jwts.builder()
                .setSubject(username)
                .claim("role",role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // extract username đồng thời cũng kiểm tra chữ kí và exp lun
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)     // verify signature + exp
                .getBody()
                .getSubject();
    }

//    parseClaimsJws() sẽ ném exception:
//
//    Trường hợp	       Exception
//    Token giả	      SignatureException
//    Hết hạn	      ExpiredJwtException
//    Sai format	  MalformedJwtException
//    Key sai	      UnsupportedJwtException

    public boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername());
    }

}


package com.r2s.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;


@Component
public class JwtUtil {
    private final SecretKey accessKey;

    public JwtUtil(
            @Value("${jwt.access-key}") String accessKeyBase64) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessKeyBase64));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRoles(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        extractAllClaims(token); // nếu sai chữ ký / hết hạn → exception
        return true;
    }

}

// extract username đồng thời cũng kiểm tra chữ kí và exp lun
//    public String extractUsername(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(accessKey)
//                .build()
//                .parseClaimsJws(token)     // verify signature + exp
//                .getBody()
//                .getSubject();
//    }

//    parseClaimsJws() sẽ ném exception:
//
//    Trường hợp	       Exception
//    Token giả	      SignatureException
//    Hết hạn	      ExpiredJwtException
//    Sai format	  MalformedJwtException
//    Key sai	      UnsupportedJwtException

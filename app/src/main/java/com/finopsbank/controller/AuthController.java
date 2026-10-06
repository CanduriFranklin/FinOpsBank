package com.finopsbank.controller;

import com.finopsbank.dto.LoginRequest;
import com.finopsbank.dto.LoginResponse;
import com.finopsbank.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de autenticacion.
 * Usa usuarios en memoria como placeholder mientras no exista tabla de usuarios.
 * Para produccion, reemplazar por un UserDetailsService conectado a la BD.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserAccount> users;

    public AuthController(JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.users = Map.of(
                "admin",    new UserAccount(passwordEncoder.encode("admin123"),    "ROLE_ADMIN"),
                "teller",   new UserAccount(passwordEncoder.encode("teller123"),   "ROLE_TELLER"),
                "customer", new UserAccount(passwordEncoder.encode("customer123"), "ROLE_CUSTOMER")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        UserAccount account = users.get(request.username());
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = tokenProvider.generateToken(request.username(), account.role());
        return ResponseEntity.ok(LoginResponse.bearer(token, tokenProvider.getExpirationMs() / 1000));
    }

    private record UserAccount(String passwordHash, String role) {}
}
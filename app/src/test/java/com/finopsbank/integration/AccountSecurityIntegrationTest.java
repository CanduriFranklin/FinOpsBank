package com.finopsbank.integration;

import com.finopsbank.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("401 Unauthorized: Rechazar peticiones sin token JWT")
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/ACC-12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 Bad Request: Rechazar Payload con datos invalidos o inyeccion malformada")
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        String validToken = jwtTokenProvider.generateToken(auth);

        String invalidJsonPayload = """
            {
                "sourceAccountNumber": "INVALID_ACC",
                "targetAccountNumber": "ACC-99999999",
                "amount": -500.00
            }
            """;

        mockMvc.perform(post("/api/v1/transactions/transfer")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload))
                .andExpect(status().isBadRequest());
    }
}
package com.tfg.tfg_redsocial.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.tfg_redsocial.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de seguridad JWT.
 *
 * Verifican que:
 *   - Los endpoints protegidos rechazan peticiones sin token.
 *   - Un token falso es rechazado.
 *   - Un token válido permite acceder.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final String TEST_EMAIL    = "testsec_" + System.currentTimeMillis() + "@test.com";
    private final String TEST_USERNAME = "tsec" + (System.currentTimeMillis() % 10000);
    private final String TEST_PASSWORD = "pass123";

    private String validToken;

    @BeforeEach
    void setup() throws Exception {
        // Registrar usuario de prueba y extraer el token para los tests
        Map<String, String> body = Map.of(
                "username",    TEST_USERNAME,
                "displayName", "Security Test User",
                "email",       TEST_EMAIL,
                "password",    TEST_PASSWORD
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        validToken = (String) responseMap.get("token");
    }

    @AfterEach
    void cleanup() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    // ─── TEST 1: Endpoint protegido sin token devuelve 401 o 403 ──────────────

    @Test
    void accesoSinTokenAEndpointProtegidoDevuelveError() throws Exception {
        // /api/follows/** está bajo anyRequest().authenticated() → rechaza sin token
        mockMvc.perform(post("/api/follows/alguien"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401
                                || result.getResponse().getStatus() == 403,
                                "Se esperaba 401 o 403, se recibió: "
                                        + result.getResponse().getStatus()
                        ));
    }

    // ─── TEST 2: Token falso/manipulado devuelve error ────────────────────────

    @Test
    void tokenFalsoEsRechazado() throws Exception {
        String tokenFalso = "Bearer eyJhbGciOiJIUzI1NiJ9.PAYLOAD_FALSO.FIRMA_FALSA";

        mockMvc.perform(post("/api/follows/alguien")
                        .header("Authorization", tokenFalso))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401
                                || result.getResponse().getStatus() == 403,
                                "Token falso debería ser rechazado"
                        ));
    }

    // ─── TEST 3: Token válido permite acceder a endpoints autenticados ─────────

    @Test
    void tokenValidoPermiteAccederAlFeed() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    // ─── TEST 4: Logout vacía el contexto (sin token ya no puede operar) ──────

    @Test
    void sinTokenNoSePuedeCrearPost() throws Exception {
        // Sin Authorization header, crear un post debe ser rechazado
        // porque el Controller intenta obtener el usuario del SecurityContext
        mockMvc.perform(multipart("/api/posts")
                        .param("content", "Test post"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() >= 400,
                                "Sin token no se debe poder crear un post"
                        ));
    }
}

package com.tfg.tfg_redsocial.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.tfg_redsocial.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para AuthController.
 *
 * Se conectan a la base de datos real (PostgreSQL local) para validar
 * el flujo completo: HTTP → Controller → Service → Repository → DB.
 *
 * El usuario de prueba se crea y se borra en cada test para no dejar
 * datos basura en la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // Email único para este test. El timestamp garantiza que no choca con
    // usuarios reales ni con otras ejecuciones de los tests.
    private final String TEST_EMAIL    = "testauth_" + System.currentTimeMillis() + "@test.com";
    private final String TEST_USERNAME = "tauth" + (System.currentTimeMillis() % 10000);
    private final String TEST_PASSWORD = "pass123";

    @AfterEach
    void cleanup() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    // ─── TEST 1: Registro de usuario nuevo ────────────────────────────────────

    @Test
    void registroNuevoUsuarioDevuelve201YToken() throws Exception {
        Map<String, String> body = Map.of(
                "username",    TEST_USERNAME,
                "displayName", "Usuario Test",
                "email",       TEST_EMAIL,
                "password",    TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    // ─── TEST 2: Login con credenciales correctas ──────────────────────────────

    @Test
    void loginConCredencialesCorrectasDevuelveToken() throws Exception {
        // Primero registramos el usuario
        Map<String, String> registerBody = Map.of(
                "username",    TEST_USERNAME,
                "displayName", "Usuario Test",
                "email",       TEST_EMAIL,
                "password",    TEST_PASSWORD
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)));

        // Ahora hacemos login
        Map<String, String> loginBody = Map.of(
                "email",    TEST_EMAIL,
                "password", TEST_PASSWORD
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    // ─── TEST 3: Login con contraseña incorrecta ───────────────────────────────

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        Map<String, String> body = Map.of(
                "email",    "noexiste@test.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ─── TEST 4: Registro con email inválido devuelve 400 ─────────────────────

    @Test
    void registroConEmailInvalidoDevuelve400() throws Exception {
        Map<String, String> body = Map.of(
                "username",    "testusr",
                "displayName", "Test",
                "email",       "esto-no-es-un-email",
                "password",    "pass123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}

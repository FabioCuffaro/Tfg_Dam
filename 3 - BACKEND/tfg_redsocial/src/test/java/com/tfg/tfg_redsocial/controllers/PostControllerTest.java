package com.tfg.tfg_redsocial.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.tfg_redsocial.repositories.PostRepository;
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
 * Tests de integración para PostController.
 *
 * Verifican que:
 *   - El feed se puede obtener con un token válido.
 *   - Se puede crear un post con texto.
 *   - No se puede crear un post sin contenido (validación).
 *   - Un post recién creado se puede eliminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private final String TEST_EMAIL    = "testpost_" + System.currentTimeMillis() + "@test.com";
    private final String TEST_USERNAME = "tpost" + (System.currentTimeMillis() % 10000);
    private final String TEST_PASSWORD = "pass123";

    private String validToken;
    private Long   createdPostId;

    @BeforeEach
    void setup() throws Exception {
        Map<String, String> body = Map.of(
                "username",    TEST_USERNAME,
                "displayName", "Post Test User",
                "email",       TEST_EMAIL,
                "password",    TEST_PASSWORD
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        Map<?, ?> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        validToken = (String) responseMap.get("token");
    }

    @AfterEach
    void cleanup() {
        // Borrar el post de prueba si quedó en la base de datos
        if (createdPostId != null) {
            postRepository.findById(createdPostId).ifPresent(postRepository::delete);
        }
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    // ─── TEST 1: Obtener el feed global ───────────────────────────────────────

    @Test
    void obtenerFeedConTokenValidoDevuelve200() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(0));
    }

    // ─── TEST 2: Crear un post con texto ──────────────────────────────────────

    @Test
    void crearPostConContenidoValidoDevuelve201() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/posts")
                        .param("content", "Post de prueba desde test automatizado")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Post de prueba desde test automatizado"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        // Guardar el ID para limpiarlo en @AfterEach
        Map<?, ?> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        createdPostId = Long.valueOf(responseMap.get("id").toString());
    }

    // ─── TEST 3: Crear un post sin contenido devuelve error ───────────────────

    @Test
    void crearPostSinContenidoDevuelveError() throws Exception {
        mockMvc.perform(multipart("/api/posts")
                        .param("content", "")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() >= 400,
                                "Post sin contenido no debería aceptarse"
                        ));
    }

    // ─── TEST 4: Crear y eliminar un post ─────────────────────────────────────

    @Test
    void crearYEliminarPostFlujoCompleto() throws Exception {
        // Crear
        MvcResult createResult = mockMvc.perform(multipart("/api/posts")
                        .param("content", "Post temporal para test de borrado")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> responseMap = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Map.class);
        Long postId = Long.valueOf(responseMap.get("id").toString());

        // Borrar
        mockMvc.perform(delete("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }
}

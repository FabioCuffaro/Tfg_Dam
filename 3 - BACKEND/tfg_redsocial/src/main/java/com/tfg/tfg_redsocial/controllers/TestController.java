package com.tfg.tfg_redsocial.controllers;


import com.tfg.tfg_redsocial.repositories.PostRepository;
import com.tfg.tfg_redsocial.repositories.ProfileRepository;
import com.tfg.tfg_redsocial.repositories.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  TestController — SOLO PARA PRUEBAS. BORRAR AL TERMINAR  ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Pruebas para ver que el Servidor funciona
 */
@RestController
@RequestMapping("/test")
public class TestController {


    // ──────────────────────────────────────────────────────
    // GET /test/ping (1ª Conexión)
    // ──────────────────────────────────────────────────────
    @GetMapping("/ping")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("servidor", "Activo");
        response.put("status", "Sin fallos");
        return response;
    }

    // Records: clases de respuesta en una línea. En vez de crear los DTOS específicos para cada uno y que quede más limpio
    // Jackson las convierte a JSON automáticamente.
    record UserDTO(Long id, String email, String role) {}
    record ProfileDTO(Long id, String username, String displayName, String bio, String location) {}
    record PostDTO(Long id, String content) {}




    private final UserRepository    userRepository;
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;

    public TestController(UserRepository userRepository,
                          ProfileRepository profileRepository,
                          PostRepository postRepository) {
        this.userRepository    = userRepository;
        this.profileRepository = profileRepository;
        this.postRepository    = postRepository;
    }

    // GET /test/users
    @GetMapping("/users")
    public List<UserDTO> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getEmail(), u.getRole().name()))
                .toList();
    }

    // GET /test/profiles
    @GetMapping("/profiles")
    public List<ProfileDTO> getProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(p -> new ProfileDTO(
                        p.getId(),
                        p.getUsername(),
                        p.getDisplayName(),
                        p.getBio(),
                        p.getLocation()))
                .toList();
    }

    // GET /test/posts
    @GetMapping("/posts")
    public List<PostDTO> getPosts() {
        return postRepository.findAll()
                .stream()
                .map( p -> new PostDTO(
                        p.getId(),
                        p.getContent()
                        ))
                .toList();
    }
}


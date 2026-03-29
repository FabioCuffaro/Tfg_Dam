package com.tfg.tfg_redsocial.controllers;

import com.tfg.tfg_redsocial.dtos.ProfileResponse;
import com.tfg.tfg_redsocial.dtos.UpdateProfileRequest;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProfileController - Endpoints para perfiles
 *
 * Rutas:
 *   GET  /api/profiles/search?q=query        → Buscar usuarios (para la navbar)
 *   GET  /api/profiles/{username}             → Ver perfil público
 *   GET  /api/profiles/{username}/followers   → Lista de seguidores del usuario
 *   GET  /api/profiles/{username}/following   → Lista de usuarios a los que sigue
 *   PUT  /api/profiles/me                    → Editar propio perfil
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ---------------------- GET /api/profiles/search?q=query ----------------------

    /**
     * Busca usuarios por username o nombre. Máximo 10 resultados.
     * Usado por el buscador de la navbar.
     *
     * IMPORTANTE: Este endpoint debe estar ANTES de /{username}
     * para que Spring no interprete "search" como un username.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProfileResponse>> search(@RequestParam String q) {
        User currentUser = getCurrentUser();
        List<ProfileResponse> results = profileService.searchProfiles(q, currentUser);
        return ResponseEntity.ok(results);
    }

    // ---------------------- GET /api/profiles/{username} ----------------------

    /**
     * Ver el perfil público de cualquier usuario por su username.
     */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String username) {
        User currentUser = getCurrentUser();
        ProfileResponse profile = profileService.getProfileByUsername(username, currentUser);
        return ResponseEntity.ok(profile);
    }

    // ---------------------- GET /api/profiles/{username}/followers ----------------------

    /**
     * Lista de seguidores del usuario con ese username.
     *
     * ¿Por qué {username}/followers antes de {username}?
     * Spring MVC da prioridad a rutas más específicas, por lo que
     * "/fabio/followers" no se confundirá con el endpoint "/{username}".
     */
    @GetMapping("/{username}/followers")
    public ResponseEntity<List<ProfileResponse>> getFollowers(@PathVariable String username) {
        User currentUser = getCurrentUser();
        List<ProfileResponse> followers = profileService.getFollowers(username, currentUser);
        return ResponseEntity.ok(followers);
    }

    // ---------------------- GET /api/profiles/{username}/following ----------------------

    /**
     * Lista de usuarios a los que sigue el usuario con ese username.
     */
    @GetMapping("/{username}/following")
    public ResponseEntity<List<ProfileResponse>> getFollowing(@PathVariable String username) {
        User currentUser = getCurrentUser();
        List<ProfileResponse> following = profileService.getFollowing(username, currentUser);
        return ResponseEntity.ok(following);
    }

    // ---------------------- PUT /api/profiles/me ----------------------

    /**
     * Editar el propio perfil.
     * Solo afecta al usuario autenticado (no se puede editar el perfil de otro).
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = getCurrentUser();
        ProfileResponse updated = profileService.updateProfile(currentUser, request);
        return ResponseEntity.ok(updated);
    }
}

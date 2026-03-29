package com.tfg.tfg_redsocial.controllers;

import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.services.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * FollowController - Endpoints para seguir/dejar de seguir
 *
 * Rutas:
 *   POST   /api/follows/{username}  → Seguir a un usuario
 *   DELETE /api/follows/{username}  → Dejar de seguir
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ---------------------- POST /api/follows/{username} ----------------------

    /**
     * El usuario autenticado sigue al usuario con el username indicado.
     * Devuelve 200 OK. Es idempotente: si ya sigue, no hace nada.
     */
    @PostMapping("/{username}")
    public ResponseEntity<Void> follow(@PathVariable String username) {
        User currentUser = getCurrentUser();
        followService.follow(currentUser, username);
        return ResponseEntity.ok().build();
    }

    // ---------------------- DELETE /api/follows/{username} ----------------------

    /**
     * El usuario autenticado deja de seguir al usuario con el username indicado.
     * Devuelve 204 No Content.
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> unfollow(@PathVariable String username) {
        User currentUser = getCurrentUser();
        followService.unfollow(currentUser, username);
        return ResponseEntity.noContent().build();
    }
}

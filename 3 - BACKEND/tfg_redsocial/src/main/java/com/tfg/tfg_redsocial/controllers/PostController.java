package com.tfg.tfg_redsocial.controllers;

import com.tfg.tfg_redsocial.dtos.PostResponse;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.services.PostService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * PostController - Endpoints para posts y likes
 *
 * Todos requieren token JWT (están bajo /api/posts que es .authenticated() en SecurityConfig).
 *
 * Rutas:
 *   GET    /api/posts?page=0            → Feed global paginado
 *   GET    /api/posts/user/{userId}     → Posts de un usuario
 *   POST   /api/posts  (multipart)     → Crear post (con imagen opcional)
 *   DELETE /api/posts/{id}             → Eliminar post
 *   POST   /api/posts/{id}/like        → Toggle like/unlike
 */
@RestController
@RequestMapping("/api/posts")
@Validated
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // ---------------------- Obtener el usuario autenticado ----------------------

    /**
     * El JwtAuthenticationFilter pone el objeto User en el SecurityContext.
     * Aquí lo recuperamos para saber quién hace la petición.
     */
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ---------------------- GET /api/posts?page=0 ----------------------

    /**
     * Feed global: todos los posts, 20 por página, más recientes primero.
     *
     * @param page Número de página (0 = primera). Por defecto 0.
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page) {
        User currentUser = getCurrentUser();
        Page<PostResponse> feed = postService.getFeed(currentUser, page);
        return ResponseEntity.ok(feed);
    }

    // ---------------------- GET /api/posts/user/{userId}?page=0 ----------------------

    /**
     * Posts de un usuario específico (para su página de perfil).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page) {
        User currentUser = getCurrentUser();
        Page<PostResponse> posts = postService.getUserPosts(userId, currentUser, page);
        return ResponseEntity.ok(posts);
    }

    // ---------------------- POST /api/posts (multipart/form-data) ----------------------

    /**
     * Crear un nuevo post con imagen opcional.
     *
     * ¿Por qué multipart/form-data en vez de JSON?
     * JSON solo puede transportar texto. Para enviar un archivo binario
     * (la imagen) junto al texto del post en una sola petición,
     * necesitamos multipart/form-data, que permite mezclar campos
     * de texto y archivos binarios en el mismo cuerpo HTTP.
     *
     * @RequestParam String content       → el texto del post
     * @RequestParam MultipartFile image  → la imagen (required=false → es opcional)
     *
     * consumes = MULTIPART_FORM_DATA_VALUE → le dice a Spring que este endpoint
     * espera multipart. Sin esto, Spring no procesaría el archivo correctamente.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestParam
            @NotBlank(message = "El contenido del post no puede estar vacío")
            @Size(max = 500, message = "El post no puede superar 500 caracteres")
            String content,
            @RequestParam(required = false) MultipartFile image) {
        User currentUser = getCurrentUser();
        PostResponse response = postService.createPost(currentUser, content, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---------------------- DELETE /api/posts/{id} ----------------------

    /**
     * Eliminar un post.
     * Solo el autor o un ADMIN puede eliminarlo.
     * Devuelve 204 No Content si tiene éxito.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        postService.deletePost(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------- POST /api/posts/{id}/like ----------------------

    /**
     * Toggle like: si no tiene like lo da, si ya tiene like lo quita.
     * Devuelve el post actualizado con el nuevo conteo de likes.
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<PostResponse> toggleLike(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        PostResponse response = postService.toggleLike(currentUser, id);
        return ResponseEntity.ok(response);
    }
}

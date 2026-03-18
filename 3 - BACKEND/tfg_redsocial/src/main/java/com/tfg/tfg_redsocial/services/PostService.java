package com.tfg.tfg_redsocial.services;

import com.tfg.tfg_redsocial.dtos.PostRequest;
import com.tfg.tfg_redsocial.dtos.PostResponse;
import com.tfg.tfg_redsocial.exception.ResourceNotFoundException;
import com.tfg.tfg_redsocial.models.Like;
import com.tfg.tfg_redsocial.models.Post;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.repositories.LikeRepository;
import com.tfg.tfg_redsocial.repositories.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * PostService - Lógica de negocio para posts y likes
 *
 * Gestiona:
 *   - Feed global paginado (20 posts por página)
 *   - Posts de un usuario (para su perfil)
 *   - Crear post
 *   - Eliminar post (solo el autor o un admin)
 *   - Toggle like/unlike
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CloudinaryService cloudinaryService;

    public PostService(PostRepository postRepository,
                       LikeRepository likeRepository,
                       CloudinaryService cloudinaryService) {
        this.postRepository    = postRepository;
        this.likeRepository    = likeRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // =================================================================
    // GET /api/posts?page=0 → Feed global paginado
    // =================================================================

    /**
     * Devuelve el feed global paginado (20 posts por página, más recientes primero).
     *
     * El JOIN FETCH en PostRepository evita el problema N+1:
     * en vez de hacer 1 query para los posts + N queries para el autor de cada uno,
     * hace UNA SOLA query con JOIN que trae todo de golpe.
     *
     * @param currentUser El usuario logueado (para saber si ya dio like a cada post)
     * @param page        Número de página (empieza en 0)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(User currentUser, int page) {
        // PageRequest.of(page, 20) → página N, 20 elementos por página
        Page<Post> posts = postRepository.findAllWithAuthor(PageRequest.of(page, 20));
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
    }

    // =================================================================
    // GET /api/posts/user/{userId}?page=0 → Posts de un usuario
    // =================================================================

    /**
     * Devuelve los posts de un usuario específico (para su página de perfil).
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long targetUserId, User currentUser, int page) {
        Page<Post> posts = postRepository.findByUserId(targetUserId, PageRequest.of(page, 20));
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
    }

    // =================================================================
    // POST /api/posts → Crear un post
    // =================================================================

    /**
     * Crea un nuevo post para el usuario autenticado.
     *
     * @param currentUser El usuario logueado
     * @param content     El texto del post (obligatorio)
     * @param image       Imagen opcional (puede ser null si no se adjunta ninguna)
     *
     * Flujo con imagen:
     *   1. CloudinaryService sube el archivo → devuelve la URL HTTPS
     *   2. Guardamos esa URL en el campo image_url del post
     *
     * Flujo sin imagen:
     *   1. imageUrl queda null → el post se guarda sin imagen
     *
     * @Transactional: si la subida a Cloudinary tiene éxito pero falla el save()
     * en PostgreSQL, no tenemos rollback en Cloudinary (API externa), pero al menos
     * el post no queda guardado en un estado inconsistente en la BD.
     */
    @Transactional
    public PostResponse createPost(User currentUser, String content, MultipartFile image) {
        // Subir imagen a Cloudinary solo si viene en la petición
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(image);
        }

        Post post = Post.builder()
                .user(currentUser)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        Post saved = postRepository.save(post);
        return toPostResponse(saved, currentUser.getId());
    }

    // =================================================================
    // DELETE /api/posts/{postId} → Eliminar un post
    // =================================================================

    /**
     * Elimina un post.
     * Solo puede hacerlo el autor del post o un ADMIN.
     */
    @Transactional
    public void deletePost(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post no encontrado con id: " + postId));

        // Verificar que el usuario es el autor o es admin
        boolean isOwner = post.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("No tienes permiso para eliminar este post");
        }

        postRepository.delete(post);
    }

    // =================================================================
    // POST /api/posts/{postId}/like → Toggle like/unlike
    // =================================================================

    /**
     * Si el usuario no ha dado like → lo da.
     * Si ya ha dado like → lo quita.
     *
     * Devuelve el conteo actualizado y el nuevo estado.
     */
    @Transactional
    public PostResponse toggleLike(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post no encontrado con id: " + postId));

        var existingLike = likeRepository.findByUserIdAndPostId(currentUser.getId(), postId);

        if (existingLike.isPresent()) {
            // Ya tiene like → lo quitamos
            likeRepository.delete(existingLike.get());
        } else {
            // No tiene like → lo añadimos
            Like like = Like.builder()
                    .user(currentUser)
                    .post(post)
                    .build();
            likeRepository.save(like);
        }

        return toPostResponse(post, currentUser.getId());
    }

    // =================================================================
    // MÉTODO PRIVADO: Convertir Post → PostResponse
    // =================================================================

    /**
     * Transforma una entidad Post en el DTO PostResponse que enviamos al frontend.
     *
     * Calcula:
     *   - likeCount: total de likes del post
     *   - likedByCurrentUser: si el usuario actual ya dio like
     */
    private PostResponse toPostResponse(Post post, Long currentUserId) {
        long likeCount = likeRepository.countByPostId(post.getId());
        boolean liked = likeRepository.existsByUserIdAndPostId(currentUserId, post.getId());

        var profile = post.getUser().getProfile();

        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getUser().getId(),
                profile != null ? profile.getUsername() : "unknown",
                profile != null ? profile.getDisplayName() : "Usuario",
                profile != null ? profile.getAvatarUrl() : null,
                likeCount,
                liked
        );
    }
}

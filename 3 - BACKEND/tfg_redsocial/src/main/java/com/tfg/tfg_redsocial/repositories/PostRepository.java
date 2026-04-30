package com.tfg.tfg_redsocial.repositories;


import com.tfg.tfg_redsocial.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Feed global paginado (Voy a poner máximo unos 20 posts por página)
    // JOIN FETCH para cargar el perfil del autor en la misma query → evita el problema N+1
    // ⭐ CRITICO: Sin esto: 1 query por post + 1 por usuario = 21 queries si hay 20 posts
    //Con esto: 1 sola query trae posts + usuarios + profiles

    //Cambio importante, va a empezar por el último post que se ha posteado, para que no influya al recargar el scroll
    @Query("SELECT p FROM Post p JOIN FETCH p.user u JOIN FETCH u.profile ORDER BY p.createdAt DESC, p.id DESC")
    Page<Post> findAllWithAuthor(Pageable pageable);

    // Posts de un usuario específico (para su página de perfil)
    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Post> findByUserId(Long userId, Pageable pageable);

    // Contar cuántos posts tiene un usuario.
    // Hibernate genera: SELECT COUNT(*) FROM posts WHERE user_id = ?
    // Lo usamos en ProfileService para las estadísticas del perfil.
    Integer countByUserId(Long userId);
}
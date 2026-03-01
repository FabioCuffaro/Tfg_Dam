package com.tfg.tfg_redsocial.repositories;


import com.tfg.tfg_redsocial.models.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // Comprobar si un usuario ya dio like a un post (para el toggle like/unlike)
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    // Contar cuantos likes tiene un post
    long countByPostId(Long postId);

    // No voy a hacer algo muy complejo con los Likes, pero si el usuario activo le da me gusta, quiero que sepa
    // o que le salga en rojo o de algún color de que él le ha dado like al post. Para diferenciarlo vaya
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
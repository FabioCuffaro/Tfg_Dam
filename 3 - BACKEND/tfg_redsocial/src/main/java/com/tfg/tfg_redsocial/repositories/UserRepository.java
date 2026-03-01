package com.tfg.tfg_redsocial.repositories;


import com.tfg.tfg_redsocial.models.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Acceso a la tabla users
 *
 * JpaRepository ya me da directamente esto:
 *   save(), findById(), findAll(), deleteById(), existsById()...
 *   Por lo que voy a escoger las necesarias para mi proyecto
 *
 * Aquí solo añadimos las consultas que necesitamos extra.
 */

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    // Para el login: buscar usuario por email
    Optional<User> findByEmail(String email);

    // Para validar en el registro que el email no exista ya
    boolean existsByEmail(String email);
}

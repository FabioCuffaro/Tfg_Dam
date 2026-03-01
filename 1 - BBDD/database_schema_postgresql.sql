-- ================================================
-- SCHEMA PARA RED SOCIAL SIMPLIFICADA
-- Base de datos: PostgreSQL
-- ================================================

-- Crear base de datos (para ejecutarla como superuser)
CREATE DATABASE social_tfg_db;
-- \c social_tfg_db;


-- ================================================
-- TABLA: users (autenticación)
-- ================================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,      -- Hasheado con BCrypt
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- 'USER' o 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- ================================================
-- TABLA: profiles (información pública de los usuarios)
-- ================================================
CREATE TABLE IF NOT EXISTS profiles (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(500),
    location VARCHAR(100),
    website VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- ================================================
-- TABLA: posts (publicaciones del feed)
-- ================================================
CREATE TABLE IF NOT EXISTS posts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_post_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- ================================================
-- TABLA: likes
-- ================================================
CREATE TABLE IF NOT EXISTS likes (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_like_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_like_post
        FOREIGN KEY(post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    UNIQUE(user_id, post_id)
);

-- ================================================
-- TABLA: follows
-- ================================================
CREATE TABLE IF NOT EXISTS follows (
    id SERIAL PRIMARY KEY,
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_follower
        FOREIGN KEY(follower_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_following
        FOREIGN KEY(following_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CHECK (follower_id != following_id),
    UNIQUE(follower_id, following_id)
);




-- ================================================
-- DATOS DE PRUEBA
-- users, profiles, luego posts.
-- ================================================

-- 1. USUARIOS
INSERT INTO users (id, email, password, role, is_active) VALUES
  (1,'fabio@gmail.com',  '123456', 'ADMIN',  true),
  (2,'maria@gmail.com',  '123456', 'USER',  true),
  (3,'carlos@gmail.com', '123456', 'USER', true);

-- 2. PERFILES (dependen de users)
INSERT INTO profiles (user_id, username, display_name, bio, location) VALUES
  (1, 'fabio',     'Fabio Cuffaro',    'Estudiante DAM y Administrador',  'Granada'),
  (2, 'maria_dev', 'María García',     'Frontend y CSS.',  'Madrid'),
  (3, 'carlos',    'Carlos Rodríguez', 'Estudiante DAM.',   'Sevilla');

-- 3. POSTS (dependen de users)
INSERT INTO posts (user_id, content) VALUES
  (1, 'Primer post de Fabio. El backend está arriba.'),
  (1, 'Segundo post de Fabio. PostgreSQL conectado.'),
  (2, 'María aquí. El frontend ya está maquetado.'),
  (3, 'Carlos el colega. Todo bajo control.');

  SELECT * FROM users;


-- ================================================
-- CONSULTAS ADMIN ÚTILES
-- ================================================

-- Ver todos los usuarios con mi rol
-- SELECT id, email, role, is_active, created_at FROM users ORDER BY created_at DESC;

-- Eliminar un usuario y todo su contenido (CASCADE borra perfil, posts, likes, follows)
-- DELETE FROM users WHERE id = id_del_usuario;

-- Eliminar un post específico (CASCADE borra sus likes)
-- DELETE FROM posts WHERE id = id_del_post;

-- Desactivar un usuario sin borrar sus datos (soft delete)
-- UPDATE users SET is_active = FALSE WHERE id = <id_del_usuario>;

-- Ver posts con info del autor para moderar
-- SELECT
--     p.id,
--     p.content,
--     p.created_at,
--     pr.username,
--     u.email,
--     u.role
-- FROM posts p
-- INNER JOIN users u ON p.user_id = u.id
-- INNER JOIN profiles pr ON p.user_id = pr.user_id
-- ORDER BY p.created_at DESC;



-- ================================================
-- CONSULTAS ÚTILES PARA TESTEAR
-- ================================================

-- Ver todos los posts con información del autor
-- SELECT
--     p.id,
--     p.content,
--     p.image_url,
--     p.created_at,
--     pr.username,
--     pr.display_name,
--     pr.avatar_url
-- FROM posts p
-- INNER JOIN profiles pr ON p.user_id = pr.user_id
-- ORDER BY p.created_at DESC;

-- Contar posts por usuario
-- SELECT
--     pr.username,
--     COUNT(p.id) as total_posts
-- FROM profiles pr
-- LEFT JOIN posts p ON pr.user_id = p.user_id
-- GROUP BY pr.username;

-- Ver seguidores de un usuario
-- SELECT
--     pr.username as follower
-- FROM follows f
-- INNER JOIN profiles pr ON f.follower_id = pr.user_id
-- WHERE f.following_id = 1;



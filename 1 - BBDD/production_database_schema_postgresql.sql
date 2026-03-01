-- ================================================
-- SCHEMA PARA RED SOCIAL SIMPLIFICADA - PROMETEO
-- Base de datos: PostgreSQL 17
-- Autor: Fabio Manuel Cuffaro Cámara
-- ================================================
-- INSTRUCCIONES DE USO:
--   Desarrollo local:
--     1. Ejecutar el bloque CREATE DATABASE
--     2. Ejecutar las tablas
--     3. Ejecutar los datos de prueba (opcional)
--
--   Producción (Railway):
--     1. Railway ya crea la base de datos automáticamente
--     2. Ejecutar SOLO las tablas
--     3. NO ejecutar los datos de prueba
--     4. Hibernate (ddl-auto=validate) verificará que el schema es correcto
-- ================================================


-- ================================================
-- BASE DE DATOS
-- Solo ejecutar en local como superuser.
-- En Railway la base de datos ya existe.
-- ================================================
-- CREATE DATABASE social_tfg_db;
-- \c social_tfg_db;


-- ================================================
-- TABLA: users
-- Gestiona autenticación y credenciales.
-- La contraseña siempre se guarda hasheada con BCrypt.
-- ================================================
CREATE TABLE IF NOT EXISTS users (
    id          SERIAL PRIMARY KEY,
    email       VARCHAR(100)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    role        VARCHAR(20)   NOT NULL DEFAULT 'USER',
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice en email: se consulta en cada login
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);


-- ================================================
-- TABLA: profiles
-- Información pública del usuario.
-- Relación 1:1 con users (ON DELETE CASCADE).
-- ================================================
CREATE TABLE IF NOT EXISTS profiles (
    id           SERIAL PRIMARY KEY,
    user_id      INT          NOT NULL UNIQUE,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    display_name VARCHAR(100),
    bio          TEXT,
    avatar_url   VARCHAR(500),
    location     VARCHAR(100),
    website      VARCHAR(200),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_profile_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Índice en username: se consulta en cada visita a perfil (/profiles/username)
CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);

-- Índice para el buscador (búsqueda por username y display_name)
CREATE INDEX IF NOT EXISTS idx_profiles_search
    ON profiles(username, display_name);


-- ================================================
-- TABLA: posts
-- Publicaciones del feed.
-- ON DELETE CASCADE: si se borra el usuario, se borran sus posts.
-- ================================================
CREATE TABLE IF NOT EXISTS posts (
    id          SERIAL PRIMARY KEY,
    user_id     INT       NOT NULL,
    content     TEXT      NOT NULL,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_post_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Índice en created_at DESC: el feed siempre se ordena por fecha descendente
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);

-- Índice en user_id: para cargar los posts de un usuario en su perfil
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);


-- ================================================
-- TABLA: likes
-- Relación N:M entre users y posts.
-- UNIQUE(user_id, post_id): un usuario no puede dar like dos veces al mismo post.
-- ================================================
CREATE TABLE IF NOT EXISTS likes (
    id          SERIAL PRIMARY KEY,
    user_id     INT       NOT NULL,
    post_id     INT       NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_like_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_like_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_like UNIQUE(user_id, post_id)
);

-- Índice compuesto: existsByUserIdAndPostId y findByUserIdAndPostId
-- son las consultas más frecuentes en LikeRepository
CREATE INDEX IF NOT EXISTS idx_likes_user_post ON likes(user_id, post_id);


-- ================================================
-- TABLA: follows
-- Relación N:M entre users (seguidor → seguido).
-- CHECK: no puedes seguirte a ti mismo.
-- UNIQUE: no puedes seguir a la misma persona dos veces.
-- ================================================
CREATE TABLE IF NOT EXISTS follows (
    id           SERIAL PRIMARY KEY,
    follower_id  INT       NOT NULL,
    following_id INT       NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_follower
        FOREIGN KEY (follower_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_following
        FOREIGN KEY (following_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_no_self_follow
        CHECK (follower_id != following_id),

    CONSTRAINT uq_follow
        UNIQUE(follower_id, following_id)
);

-- Índice para countByFollowingId (contar seguidores de un usuario)
CREATE INDEX IF NOT EXISTS idx_follows_following_id ON follows(following_id);

-- Índice para countByFollowerId (contar a cuántos sigue un usuario)
CREATE INDEX IF NOT EXISTS idx_follows_follower_id ON follows(follower_id);


-- ================================================
-- DATOS DE PRUEBA
-- ================================================
-- ⚠️  SOLO PARA DESARROLLO LOCAL. NUNCA EN PRODUCCIÓN.
--
-- Las contraseñas son '123456' hasheadas con BCrypt (coste 10).
-- Se pueden usar directamente para hacer login en Postman o en el navegador.
--
-- IMPORTANTE: no especificamos el campo id para que PostgreSQL
-- use la secuencia automática y no haya colisiones al registrar
-- usuarios nuevos desde la aplicación.
-- ================================================

/*

-- 1. USUARIOS (sin id manual, sin contraseña en texto plano)
INSERT INTO users (email, password, role, is_active) VALUES
  ('fabio@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true),
  ('maria@gmail.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER',  true),
  ('carlos@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER',  true);

-- 2. PERFILES (usamos subconsulta para obtener el user_id sin hardcodearlo)
INSERT INTO profiles (user_id, username, display_name, bio, location) VALUES
  ((SELECT id FROM users WHERE email = 'fabio@gmail.com'),  'fabio',     'Fabio Cuffaro',    'Estudiante DAM y Administrador', 'Granada'),
  ((SELECT id FROM users WHERE email = 'maria@gmail.com'),  'maria_dev', 'María García',     'Frontend y CSS.',                'Madrid'),
  ((SELECT id FROM users WHERE email = 'carlos@gmail.com'), 'carlos',    'Carlos Rodríguez', 'Estudiante DAM.',                'Sevilla');

-- 3. POSTS (igual, subconsulta para el user_id)
INSERT INTO posts (user_id, content) VALUES
  ((SELECT id FROM users WHERE email = 'fabio@gmail.com'),  'Primer post de Fabio. El backend está arriba.'),
  ((SELECT id FROM users WHERE email = 'fabio@gmail.com'),  'Segundo post de Fabio. PostgreSQL conectado.'),
  ((SELECT id FROM users WHERE email = 'maria@gmail.com'),  'María aquí. El frontend ya está maquetado.'),
  ((SELECT id FROM users WHERE email = 'carlos@gmail.com'), 'Carlos el colega. Todo bajo control.');

*/


-- ================================================
-- CONSULTAS ADMIN ÚTILES
-- ================================================

-- Ver todos los usuarios con su rol
-- SELECT id, email, role, is_active, created_at FROM users ORDER BY created_at DESC;

-- Promover un usuario a ADMIN
-- UPDATE users SET role = 'ADMIN' WHERE email = 'email@ejemplo.com';

-- Desactivar un usuario sin borrar sus datos (soft delete)
-- UPDATE users SET is_active = FALSE WHERE id = <id>;

-- Eliminar un usuario y todo su contenido (CASCADE borra perfil, posts, likes, follows)
-- DELETE FROM users WHERE id = <id>;

-- Eliminar un post específico (CASCADE borra sus likes)
-- DELETE FROM posts WHERE id = <id>;

-- Ver posts con info del autor (útil para moderar)
-- SELECT p.id, p.content, p.created_at, pr.username, u.email, u.role
-- FROM posts p
-- INNER JOIN users u    ON p.user_id = u.id
-- INNER JOIN profiles pr ON p.user_id = pr.user_id
-- ORDER BY p.created_at DESC;


-- ================================================
-- CONSULTAS ÚTILES PARA TESTEAR
-- ================================================

-- Ver todos los posts con información del autor
-- SELECT p.id, p.content, p.image_url, p.created_at,
--        pr.username, pr.display_name, pr.avatar_url
-- FROM posts p
-- INNER JOIN profiles pr ON p.user_id = pr.user_id
-- ORDER BY p.created_at DESC;

-- Contar posts por usuario
-- SELECT pr.username, COUNT(p.id) as total_posts
-- FROM profiles pr
-- LEFT JOIN posts p ON pr.user_id = p.user_id
-- GROUP BY pr.username
-- ORDER BY total_posts DESC;

-- Ver seguidores de un usuario concreto
-- SELECT pr.username as follower
-- FROM follows f
-- INNER JOIN profiles pr ON f.follower_id = pr.user_id
-- WHERE f.following_id = (SELECT id FROM users WHERE email = 'fabio@gmail.com');

-- Resetear secuencias si has insertado IDs manuales (solo desarrollo)
-- SELECT setval('users_id_seq',    (SELECT MAX(id) FROM users));
-- SELECT setval('profiles_id_seq', (SELECT MAX(id) FROM profiles));
-- SELECT setval('posts_id_seq',    (SELECT MAX(id) FROM posts));

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
CREATE DATABASE social_tfg_db;
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
-- Las contraseñas son '123456'.
-- No se puede acceder a ellas puesto que no están hasheadas
--
-- IMPORTANTE: no especificamos el campo id para que PostgreSQL
-- use la secuencia automática y no haya colisiones al registrar
-- usuarios nuevos desde la aplicación.
-- ================================================



-- ================================================
-- USUARIOS DE PRUEBA — IDs 1 al 4
-- ================================================

INSERT INTO users (id, email, password, role, is_active) VALUES
  (1, 'lucia@gmail.com',    '123156', 'USER', true),
  (2, 'adrian@gmail.com',   '123156', 'USER', true),
  (3, 'sara@gmail.com',     '123156', 'USER', true),
  (4, 'miguel@gmail.com',   '123156', 'USER', true);

-- ================================================
-- PERFILES
-- ================================================

INSERT INTO profiles (user_id, username, display_name, bio, location, website) VALUES
  (1, 'lucia_code',   'Lucía Fernández',  'Apasionada del diseño UX y el café ☕. Estudiante de DAW en Granada.',          'Granada',   'https://lucia.dev'),
  (2, 'adrian_dev',   'Adrián Torres',    'Backend lover. Spring Boot, Docker y mucho Stack Overflow 🐳.',                 'Valencia',  'https://adriantorres.io'),
  (3, 'sara_pixels',  'Sara Molina',      'Diseñadora gráfica metida a programadora. CSS es mi terapia 🎨.',               'Barcelona', 'https://saramolina.es'),
  (4, 'miguel_stack', 'Miguel Ángel Ruiz','Full stack en proceso. De 0 a producción paso a paso. Café y código 🚀.',       'Madrid',    'https://miguelruiz.dev');

-- ================================================
-- POSTS — mínimo 1 por usuario
-- ================================================

-- Lucía (user_id = 1)
INSERT INTO posts (user_id, content) VALUES
  (1, 'Primer día usando Figma en serio y ya no puedo vivir sin los Auto Layout. ¿Por qué nadie me lo dijo antes? 😅'),
  (1, 'Tip de CSS: si usas gap en un flexbox te ahorras todos los margin-right de los hijos. Pequeñas cosas que cambian la vida.'),
  (1, 'Llevaba tres horas buscando el bug. Era una coma de más en el JSON. Siempre es una coma de más.'),
  (1, 'Acabo de desplegar mi primer proyecto en Vercel y funciona a la primera. Esto no puede ser real, algo está mal 🤔'),
  (1, 'Reminder: el diseño responsive no es opcional, es el mínimo. Si tu web se rompe en móvil, tu web está rota.');

-- Adrián (user_id = 2)
INSERT INTO posts (user_id, content) VALUES
  (2, 'Spring Security me tiene bloqueado desde el lunes. El 103 más misterioso de mi vida. Alguien que haya peleado con CORS que me escriba.'),
  (2, 'Docker Compose para el entorno de desarrollo es lo mejor que me ha pasado este año. Un solo comando y todo arriba. Magia pura.'),
  (2, 'Regla de oro: nunca hagas un DELETE sin WHERE en producción. Lo digo por experiencia propia. No preguntéis.'),
  (2, 'Hoy he aprendido qué es el problema N+1 en JPA y ahora entiendo por qué las queries tardaban tanto. JOIN FETCH al rescate 🔥'),
  (2, 'PostgreSQL > MySQL. Lo he dicho. No voy a debatir esto a estas horas de la noche.');

-- Sara (user_id = 3)
INSERT INTO posts (user_id, content) VALUES
  (3, 'Hay dos tipos de personas: las que usan variables CSS y las que copian el color hexadecimal en cada selector. Sé la primera.'),
  (3, 'Hoy he convencido a mi equipo de migrar de px a rem para accesibilidad. Pequeña victoria del día 💪'),
  (3, 'Grid o Flexbox, esa es la cuestión. Mi respuesta: Grid para el layout general, Flex para los componentes. Los dos juntos son imbatibles.'),
  (3, 'Animaciones CSS bien hechas marcan la diferencia entre una web normal y una web que la gente recuerda. No las descuidéis.'),
  (3, 'Acabo de revisar código de hace seis meses. No reconozco ni los nombres de las variables. Documentad, por favor. Por el bien de vuestro yo futuro.');

-- Miguel (user_id = 4)
INSERT INTO posts (user_id, content) VALUES
  (4, 'Semana 1 aprendiendo React: esto no tiene ningún sentido. Semana 1: esto es lo más lógico del mundo. El viaje vale la pena.'),
  (4, 'JWT me parece magia negra desde fuera y sentido común desde dentro. Hay que leer sobre cómo funciona antes de usarlo, no después.'),
  (4, 'Mito: los buenos programadores memorizan todo. Realidad: los buenos programadores saben qué buscar y cómo leer la documentación.'),
  (4, 'Git commit -m "fix" por decimoquinta vez seguida. Hoy no ha sido mi día 😅'),
  (4, 'Acabo de terminar mi primera API REST completa con autenticación. Hace seis meses no sabía ni qué era un endpoint. Seguid adelante.');

-- ================================================
-- FOLLOWS opcionales para que la app tenga más vida
-- ================================================

INSERT INTO follows (follower_id, following_id) VALUES
  (1, 2),
  (1, 4),
  (2, 1),
  (2, 3),
  (3, 1),
  (3,4),
  (4, 2),
  (4, 3)


-- ================================================
-- LIKES
-- ================================================

-- ================================================
-- LIKES ALEATORIOS (Interacción entre perfiles)
-- ================================================

INSERT INTO likes (user_id, post_id) VALUES
-- Lucía (ID 1) da likes a posts de Adrián, Sara y Miguel
(1, 6),  -- Like al post de Adrián sobre CORS
(1, 11), -- Like al post de Sara sobre variables CSS
(1, 13), -- Like al post de Sara sobre Grid/Flexbox
(1, 16), -- Like al post de Miguel sobre React
(1, 20), -- Like al post de Miguel sobre su primera API

-- Adrián (ID 2) da likes a posts de Lucía, Sara y Miguel
(2, 2),  -- Like al post de Lucía sobre el gap en Flexbox
(2, 3),  -- Like al post de Lucía sobre el bug del JSON

-- Sara (ID 3) da likes a posts de Lucía, Adrián y Miguel
(3, 1),  -- Like al post de Lucía sobre Figma
(3, 5),  -- Like al post de Lucía sobre diseño responsive
(3, 18), -- Like al post de Miguel sobre la documentación

-- Miguel (ID 4) da likes a posts de Lucía, Adrián y Sara
(4, 2),  -- Like al post de Lucía sobre el gap en Flexbox
(4, 8),  -- Like al post de Adrián sobre el DELETE sin WHERE
(4, 9)  -- Like al post de Adrián sobre el problema N+1





-- ==========================================================
-- 3. IMPORTANTE AL DESPLEGAR Y PROBAR BBDD IDS DUPLICADOS
-- ==========================================================
-- Estos comandos sincronizan los contadores automáticos (secuencias)
-- con el ID más alto que hayamos insertado manualmente arriba.
-- ¡EJECUTA ESTO SIEMPRE AL FINAL DEL SCRIPT!

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('profiles_id_seq', (SELECT MAX(id) FROM profiles));
SELECT setval('posts_id_seq', (SELECT MAX(id) FROM posts));
SELECT setval('likes_id_seq', (SELECT MAX(id) FROM likes));
SELECT setval('follows_id_seq', (SELECT MAX(id) FROM follows));












-- Verificar que todo se insertó bien
SELECT u.id, u.email, u.role, p.username, p.display_name, p.location
FROM users u
INNER JOIN profiles p ON u.id = p.user_id
ORDER BY u.id;

SELECT p.id, pr.username, LEFT(p.content, 60) AS preview
FROM posts p
INNER JOIN profiles pr ON p.user_id = pr.user_id
ORDER BY p.user_id, p.id;



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

// ==========================================
// profile.js - Perfil conectado al backend
// ==========================================

const API_URL_PROFILE = 'http://localhost:8080/api';

// Username del perfil que se está viendo actualmente.
// Se guarda aquí para que initFollowListModal pueda usarlo sin parámetros.
let currentProfileUsername = null;

// ==========================================
// ARRANQUE
// ==========================================

document.addEventListener('DOMContentLoaded', function () {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initNavbar();
    loadProfile();
    initEditModal();
    initFollowListModal();
    initLogoutProfile();
});

// ==========================================
// NAVBAR
// ==========================================

function initNavbar() {
    const displayName = localStorage.getItem('displayName') || 'Usuario';
    const username    = localStorage.getItem('username') || 'user';
    const initial     = displayName.charAt(0).toUpperCase();

    document.getElementById('navUsername').textContent = '@' + username;
    const navAvatar = document.getElementById('navAvatar');
    if (navAvatar) navAvatar.textContent = initial;
}

// ==========================================
// CARGAR PERFIL DEL BACKEND
// ==========================================

async function loadProfile() {
    const params   = new URLSearchParams(window.location.search);
    const username = params.get('user') || localStorage.getItem('username');

    try {
        const response = await fetch(API_URL_PROFILE + '/profiles/' + username, {
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (response.status === 401) { logoutProfile(); return; }

        if (!response.ok) {
            showProfileError('Perfil no encontrado');
            return;
        }

        const profile = await response.json();
        const isOwnProfile = profile.username === localStorage.getItem('username');

        renderProfile(profile, isOwnProfile);
        loadUserPostsProfile(profile.userId, isOwnProfile);

    } catch (err) {
        showProfileError('No se puede conectar con el servidor');
        console.error(err);
    }
}

function renderProfile(profile, isOwnProfile) {
    document.title = (profile.displayName || profile.username) + ' - Prometeo';

    const initial = (profile.displayName || profile.username || 'U').charAt(0).toUpperCase();
    const profileAvatar = document.getElementById('profileAvatar');
    if (profile.avatarUrl) {
        profileAvatar.innerHTML = '<img src="' + profile.avatarUrl + '" alt="avatar">';
    } else {
        profileAvatar.textContent = initial;
    }

    document.getElementById('profileDisplayName').textContent = profile.displayName || profile.username;
    document.getElementById('profileUsername').textContent    = '@' + profile.username;
    document.getElementById('profileBio').textContent        = profile.bio || '';

    const locationEl = document.getElementById('profileLocation');
    if (profile.location) {
        locationEl.textContent   = '📍 ' + profile.location;
        locationEl.style.display = 'inline';
    } else {
        locationEl.style.display = 'none';
    }

    const websiteEl = document.getElementById('profileWebsite');
    if (profile.website) {
        websiteEl.textContent   = '🔗 ' + profile.website.replace('https://', '');
        websiteEl.href          = profile.website;
        websiteEl.style.display = 'inline';
    } else {
        websiteEl.style.display = 'none';
    }

    document.getElementById('statPosts').textContent     = profile.postCount || 0;
    document.getElementById('statFollowers').textContent = profile.followerCount || 0;
    document.getElementById('statFollowing').textContent = profile.followingCount || 0;

    // Guardamos el username para que el modal de seguidores sepa a quién consultar
    currentProfileUsername = profile.username;

    const editBtn   = document.getElementById('editProfileBtn');
    const followBtn = document.getElementById('followBtn');

    if (isOwnProfile) {
        editBtn.style.display   = 'block';
        followBtn.style.display = 'none';
    } else {
        editBtn.style.display   = 'none';
        followBtn.style.display = 'block';

        // Estado inicial del botón seguir
        if (profile.isFollowedByCurrentUser) {
            followBtn.textContent = 'Siguiendo';
            followBtn.classList.add('following');
        } else {
            followBtn.textContent = 'Seguir';
            followBtn.classList.remove('following');
        }

        // Guardar username para el toggle
        followBtn.dataset.username = profile.username;
        followBtn.dataset.following = profile.isFollowedByCurrentUser ? 'true' : 'false';
        followBtn.removeEventListener('click', handleFollowClick);
        followBtn.addEventListener('click', handleFollowClick);
    }
}

// ==========================================
// FOLLOW / UNFOLLOW
// ==========================================

async function handleFollowClick() {
    const btn       = document.getElementById('followBtn');
    const username  = btn.dataset.username;
    const isFollowing = btn.dataset.following === 'true';

    try {
        const method   = isFollowing ? 'DELETE' : 'POST';
        const response = await fetch(API_URL_PROFILE + '/follows/' + username, {
            method: method,
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (response.ok || response.status === 204) {
            const followersEl = document.getElementById('statFollowers');
            const currentCount = parseInt(followersEl.textContent) || 0;

            if (isFollowing) {
                btn.textContent = 'Seguir';
                btn.classList.remove('following');
                btn.dataset.following = 'false';
                followersEl.textContent = Math.max(0, currentCount - 1);
            } else {
                btn.textContent = 'Siguiendo';
                btn.classList.add('following');
                btn.dataset.following = 'true';
                followersEl.textContent = currentCount + 1;
            }
        }
    } catch (err) {
        console.error(err);
    }
}

// ==========================================
// POSTS DEL USUARIO EN SU PERFIL
// ==========================================

async function loadUserPostsProfile(userId, isOwnProfile) {
    const list = document.getElementById('profilePostsList');

    try {
        const response = await fetch(API_URL_PROFILE + '/posts/user/' + userId + '?page=0', {
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (!response.ok) {
            list.innerHTML = '<div class="empty-state"><p class="empty-state-title">Error al cargar posts</p></div>';
            return;
        }

        const data = await response.json();
        list.innerHTML = '';

        if (!data.content || data.content.length === 0) {
            const msg = isOwnProfile
                ? '¡Comparte algo con la comunidad!'
                : 'Este usuario no ha publicado nada todavía.';
            list.innerHTML =
                '<div class="empty-state">' +
                    '<div class="empty-state-icon">✏️</div>' +
                    '<p class="empty-state-title">Aún no hay posts</p>' +
                    '<p class="empty-state-text">' + msg + '</p>' +
                '</div>';
            return;
        }

        data.content.forEach(function (post) {
            const temp = document.createElement('div');
            temp.innerHTML = createPostHTML(post);
            const card = temp.firstElementChild;
            list.appendChild(card);
            attachPostEvents(post.id);
        });

    } catch (err) {
        list.innerHTML = '<div class="empty-state"><p class="empty-state-title">Error al cargar posts</p></div>';
        console.error(err);
    }
}

// ==========================================
// MODAL EDITAR PERFIL
// ==========================================

function initEditModal() {
    const modal     = document.getElementById('editModal');
    const openBtn   = document.getElementById('editProfileBtn');
    const closeBtn  = document.getElementById('closeEditModal');
    const cancelBtn = document.getElementById('cancelEdit');
    const saveBtn   = document.getElementById('saveEdit');

    if (!modal || !openBtn) return;

    openBtn.addEventListener('click', function () {
        document.getElementById('editDisplayName').value = document.getElementById('profileDisplayName').textContent;
        document.getElementById('editBio').value         = document.getElementById('profileBio').textContent;

        const locEl = document.getElementById('profileLocation');
        document.getElementById('editLocation').value = locEl.style.display !== 'none'
            ? locEl.textContent.replace('📍 ', '') : '';

        const webEl = document.getElementById('profileWebsite');
        document.getElementById('editWebsite').value = webEl.style.display !== 'none'
            ? webEl.href : '';

        modal.style.display = 'flex';
    });

    function closeModal() { modal.style.display = 'none'; }
    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', function (e) { if (e.target === modal) closeModal(); });

    saveBtn.addEventListener('click', handleSaveProfile);
}

async function handleSaveProfile() {
    const displayName = document.getElementById('editDisplayName').value.trim();
    const bio         = document.getElementById('editBio').value.trim();
    const location    = document.getElementById('editLocation').value.trim();
    const website     = document.getElementById('editWebsite').value.trim();

    if (!displayName) { alert('El nombre es obligatorio'); return; }

    try {
        const response = await fetch(API_URL_PROFILE + '/profiles/me', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            body: JSON.stringify({ displayName, bio, location, website })
        });

        if (!response.ok) {
            alert('Error al guardar los cambios.');
            return;
        }

        const updated = await response.json();

        // Actualizar localStorage con el nuevo displayName
        localStorage.setItem('displayName', updated.displayName);

        // Re-renderizar el perfil con los nuevos datos
        renderProfile(updated, true);

        document.getElementById('editModal').style.display = 'none';

    } catch (err) {
        alert('No se puede conectar con el servidor.');
        console.error(err);
    }
}

// ==========================================
// MODAL DE SEGUIDORES / SIGUIENDO
// ==========================================

/**
 * Inicializa los botones de stats (Seguidores y Siguiendo) para que al
 * hacer clic abran el modal y carguen la lista del backend.
 *
 * ¿Por qué initFollowListModal en lugar de listeners directos en renderProfile?
 * Porque el modal y su botón de cierre se inicializan UNA SOLA VEZ al cargar
 * la página. Los datos del perfil (username) los leemos de currentProfileUsername,
 * que se actualiza cada vez que se renderiza un perfil.
 */
function initFollowListModal() {
    const modal    = document.getElementById('followListModal');
    const closeBtn = document.getElementById('closeFollowListModal');

    // Cerrar con el botón ✕
    closeBtn.addEventListener('click', function () {
        modal.style.display = 'none';
    });

    // Cerrar haciendo clic en el fondo oscuro
    modal.addEventListener('click', function (e) {
        if (e.target === modal) modal.style.display = 'none';
    });

    // Cerrar con Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal.style.display !== 'none') {
            modal.style.display = 'none';
        }
    });

    // Botón "Seguidores" → carga la lista de seguidores del perfil actual
    document.getElementById('statFollowersBtn').addEventListener('click', function () {
        if (!currentProfileUsername) return;
        openFollowModal('followers', currentProfileUsername);
    });

    // Botón "Siguiendo" → carga la lista de usuarios a los que sigue
    document.getElementById('statFollowingBtn').addEventListener('click', function () {
        if (!currentProfileUsername) return;
        openFollowModal('following', currentProfileUsername);
    });
}

/**
 * Abre el modal, pone el título correcto y llama al backend para cargar la lista.
 *
 * @param type     'followers' o 'following'
 * @param username El username del perfil que se está viendo
 */
async function openFollowModal(type, username) {
    const modal = document.getElementById('followListModal');
    const title = document.getElementById('followListTitle');
    const body  = document.getElementById('followListBody');

    // Título dinámico
    title.textContent = type === 'followers'
        ? 'Seguidores de @' + username
        : '@' + username + ' sigue a';

    // Mostrar modal con skeletons mientras carga
    body.innerHTML =
        '<div class="follow-list-skeleton"></div>' +
        '<div class="follow-list-skeleton"></div>' +
        '<div class="follow-list-skeleton"></div>';
    modal.style.display = 'flex';

    // Llamada al backend
    // GET /api/profiles/{username}/followers  o  /api/profiles/{username}/following
    const endpoint = API_URL_PROFILE + '/profiles/' + username + '/' + type;

    try {
        const response = await fetch(endpoint, {
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (!response.ok) {
            body.innerHTML = '<p class="follow-list-empty">Error al cargar la lista.</p>';
            return;
        }

        const users = await response.json();
        renderFollowList(body, users, type);

    } catch (err) {
        body.innerHTML = '<p class="follow-list-empty">No se puede conectar con el servidor.</p>';
        console.error(err);
    }
}

/**
 * Pinta la lista de usuarios dentro del modal.
 *
 * Cada fila muestra: avatar (inicial o imagen), nombre, @username.
 * Al hacer clic en cualquier fila navega al perfil de ese usuario.
 *
 * @param container El elemento del DOM donde pintar la lista
 * @param users     Array de ProfileResponse del backend
 * @param type      'followers' o 'following' (para el mensaje vacío)
 */
function renderFollowList(container, users, type) {
    if (!users || users.length === 0) {
        const emptyMsg = type === 'followers'
            ? 'Este usuario todavía no tiene seguidores.'
            : 'Este usuario todavía no sigue a nadie.';

        container.innerHTML =
            '<div class="follow-list-empty">' +
                '<div class="follow-list-empty-icon">👤</div>' +
                '<p>' + emptyMsg + '</p>' +
            '</div>';
        return;
    }

    container.innerHTML = users.map(function (user) {
        const initial = (user.displayName || user.username || 'U').charAt(0).toUpperCase();
        const avatar  = user.avatarUrl
            ? '<img src="' + user.avatarUrl + '" alt="avatar">'
            : initial;

        return (
            '<div class="follow-list-item" onclick="goToProfileFromModal(\'' + user.username + '\')">' +
                '<div class="follow-list-avatar">' + avatar + '</div>' +
                '<div class="follow-list-info">' +
                    '<p class="follow-list-name">' + escapeHTML(user.displayName || user.username) + '</p>' +
                    '<p class="follow-list-username">@' + escapeHTML(user.username) + '</p>' +
                '</div>' +
            '</div>'
        );
    }).join('');
}

/**
 * Navega al perfil de un usuario cerrando el modal antes.
 * Separado de goToProfile para cerrar el modal limpiamente.
 */
function goToProfileFromModal(username) {
    document.getElementById('followListModal').style.display = 'none';
    window.location.href = 'profile.html?user=' + username;
}

// ==========================================
// LOGOUT
// ==========================================

function initLogoutProfile() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function () {
            if (confirm('¿Cerrar sesión?')) logoutProfile();
        });
    }
}

function logoutProfile() {
    localStorage.clear();
    window.location.href = 'login.html';
}

// ==========================================
// NAVEGACIÓN + UTILIDADES (reutilizadas de feed.js)
// ==========================================

function goToProfile(username) {
    window.location.href = 'profile.html?user=' + username;
}

function createPostHTML(post) {
    const currentUserId = parseInt(localStorage.getItem('userId'));
    const initial      = (post.authorDisplayName || 'U').charAt(0).toUpperCase();
    const isOwner      = post.authorId === currentUserId;

    // Leemos el role que login.js guardó en localStorage tras el login.
    // AuthResponse.java lo devuelve como "ADMIN" o "USER" (sin prefijo ROLE_).
    // login.js → localStorage.setItem('role', data.role)
    const isAdmin = localStorage.getItem('role') === 'ADMIN';

    // El botón aparece si el usuario es el autor del post O si es admin.
    // El backend (PostService.deletePost) ya tiene la misma lógica y acepta
    // peticiones DELETE de admins aunque no sean el autor.

    const likedClass   = post.likedByCurrentUser ? 'liked' : '';
    const heartFill    = post.likedByCurrentUser ? 'currentColor' : 'none';

    const heartSVG =
        '<svg width="16" height="16" viewBox="0 0 24 24" fill="' + heartFill + '" stroke="currentColor" stroke-width="2">' +
            '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>' +
        '</svg>';

    const avatarContent = post.authorAvatarUrl
        ? '<img src="' + post.authorAvatarUrl + '" alt="avatar">'
        : initial;

    const imageHTML = post.imageUrl
        ? '<img src="' + post.imageUrl + '" alt="imagen" class="post-image">'
        : '';

    const deleteBtn = (isOwner || isAdmin)
        ? '<button class="btn-delete-post visible" data-post-id="' + post.id + '">🗑 Eliminar</button>'
        : '';

    return (
        '<div class="post-card" data-post-id="' + post.id + '">' +
            '<div class="post-header">' +
                '<div class="post-avatar" onclick="goToProfile(\'' + post.authorUsername + '\')">' + avatarContent + '</div>' +
                '<div>' +
                    '<p class="post-author-name" onclick="goToProfile(\'' + post.authorUsername + '\')">' + escapeHTML(post.authorDisplayName || 'Usuario') + '</p>' +
                    '<p class="post-author-username">@' + escapeHTML(post.authorUsername || '') + '</p>' +
                '</div>' +
                '<span class="post-time">' + formatTime(post.createdAt) + '</span>' +
            '</div>' +
            '<p class="post-content">' + escapeHTML(post.content) + '</p>' +
            imageHTML +
            '<div class="post-footer">' +
                '<button class="btn-like ' + likedClass + '" data-post-id="' + post.id + '">' + heartSVG + '<span class="like-count">' + post.likeCount + '</span></button>' +
                deleteBtn +
            '</div>' +
        '</div>'
    );
}

function attachPostEvents(postId) {
    const likeBtn = document.querySelector('.btn-like[data-post-id="' + postId + '"]');
    if (likeBtn) {
        likeBtn.addEventListener('click', async function () {
            const isLiked = likeBtn.classList.contains('liked');
            const countEl = likeBtn.querySelector('.like-count');
            const heart   = likeBtn.querySelector('svg');
            const count   = parseInt(countEl.textContent);

            likeBtn.classList.toggle('liked');
            heart.setAttribute('fill', isLiked ? 'none' : 'currentColor');
            countEl.textContent = isLiked ? count - 1 : count + 1;

            try {
                const res = await fetch(API_URL_PROFILE + '/posts/' + postId + '/like', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
                });
                if (!res.ok) {
                    likeBtn.classList.toggle('liked');
                    heart.setAttribute('fill', isLiked ? 'currentColor' : 'none');
                    countEl.textContent = count;
                }
            } catch (err) {
                likeBtn.classList.toggle('liked');
                heart.setAttribute('fill', isLiked ? 'currentColor' : 'none');
                countEl.textContent = count;
            }
        });
    }

    const deleteBtn = document.querySelector('.btn-delete-post[data-post-id="' + postId + '"]');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', async function () {
            if (!confirm('¿Seguro que quieres eliminar este post?')) return;
            try {
                const res = await fetch(API_URL_PROFILE + '/posts/' + postId, {
                    method: 'DELETE',
                    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
                });
                if (res.ok || res.status === 204) {
                    const card = document.querySelector('.post-card[data-post-id="' + postId + '"]');
                    if (card) card.remove();
                }
            } catch (err) { console.error(err); }
        });
    }
}

function showProfileError(msg) {
    document.getElementById('profileDisplayName').textContent = msg;
}

function formatTime(dateStr) {
    const now  = new Date();
    const date = new Date(dateStr);
    const diff = Math.floor((now - date) / 1000);
    if (diff < 60)        return 'ahora';
    if (diff < 3600)      return 'hace ' + Math.floor(diff / 60) + 'm';
    if (diff < 86400)     return 'hace ' + Math.floor(diff / 3600) + 'h';
    if (diff < 86400 * 7) return 'hace ' + Math.floor(diff / 86400) + 'd';
    return date.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

function escapeHTML(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function initLightbox() {
    const overlay   = document.getElementById('lightboxOverlay');
    const closeBtn  = document.getElementById('lightboxClose');

    // Cerrar al pulsar ✕ o al hacer clic fuera de la imagen
    closeBtn.addEventListener('click', closeLightbox);
    overlay.addEventListener('click', function (e) {
        if (e.target === overlay) closeLightbox();
    });

    // Cerrar con Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeLightbox();
    });
}

function openLightbox(src) {
    const overlay = document.getElementById('lightboxOverlay');
    const img     = document.getElementById('lightboxImg');
    img.src = src;
    overlay.classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeLightbox() {
    const overlay = document.getElementById('lightboxOverlay');
    overlay.classList.remove('active');
    document.body.style.overflow = '';
}

// ==========================================
// profile.js - Perfil conectado al backend
// ==========================================

const API_URL_PROFILE = 'http://localhost:8080/api';

// Username del perfil que se está viendo actualmente.
// Se guarda aquí para que initFollowListModal pueda usarlo sin parámetros.
let currentProfileUsername = null;


// ==========================================
// CARGAR PERFIL DEL BACKEND
// ==========================================

// ── ESTADO DEL PERFIL ──────────────────────────────────────────
// profileCurrentPage   → última página de posts de perfil cargada
// profileIsLoading     → semáforo anti-peticiones simultáneas
// profileHasMore       → ¿hay más posts en el backend?
// profileUserId        → ID del usuario cuyo perfil se está viendo
//                        (necesario para poder pedir más páginas
//                         desde initProfileInfiniteScroll sin parámetros)
// profileIsOwnProfile  → para el mensaje de estado vacío
let profileCurrentPage  = 0;
let profileIsLoading    = false;
let profileHasMore      = true;
let profileUserId       = null;
let profileIsOwnProfile = false;


// ==========================================
// ARRANQUE
// ==========================================

document.addEventListener('DOMContentLoaded', function () {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = './login.html';
        return;
    }

    initNavbar();
    initSearch();
    loadProfile();
    initEditModal();
    initFollowListModal();
    initLogoutProfile();
    initProfileInfiniteScroll();
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





async function loadProfile() {
  // Resetear estado antes del fetch asíncrono para que el IntersectionObserver
  // no dispare loadUserPostsProfile con el userId de un perfil anterior (o null)
  profileUserId      = null;
  profileCurrentPage = 0;
  profileHasMore     = true;

  const params   = new URLSearchParams(window.location.search);
  const username = params.get('user') || localStorage.getItem('username');

  try {
    const response = await fetch(API_URL_PROFILE + '/profiles/' + username, {
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
    });

    if (response.status === 401) { logoutProfile(); return; }
    if (!response.ok) { showProfileError('Perfil no encontrado'); return; }

    const profile     = await response.json();
    const isOwnProfile = profile.username === localStorage.getItem('username');

    renderProfile(profile, isOwnProfile);

    // ── GUARDAR EN VARIABLES DE MÓDULO ─────────────────────────────
    // initProfileInfiniteScroll los leerá cuando el observer se dispare.
    // Si no los guardamos aquí, el observer no sabría de qué usuario
    // pedir más posts.
    profileUserId       = profile.userId;      // ← NUEVO
    profileIsOwnProfile = isOwnProfile;        // ← NUEVO

    // Cargar la primera página de posts del perfil
    loadUserPostsProfile(0);  // ← pasa solo el número de página

  } catch (err) {
    showProfileError('No se puede conectar con el servidor');
    console.error(err);
  }
}

function renderProfile(profile, isOwnProfile) {
    document.title = (profile.displayName || profile.username) + ' - Social Web';

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

/**
 * loadUserPostsProfile(page)
 *
 * Carga una página de posts del usuario cuyo ID está en profileUserId.
 *
 * @param {number} page  Número de página a cargar (0 = primera).
 *
 * ¿Por qué solo vaciar el DOM en page === 0?
 * En las páginas siguientes queremos añadir posts a los que ya están,
 * no reemplazarlos. Vaciar el DOM en page > 0 destruiría el scroll
 * que el usuario ya tenía.
 */
async function loadUserPostsProfile(page) {
  if (profileIsLoading) return;
  if (!profileHasMore && page !== 0) return;
  // Guard: si profileUserId aún no está listo (loadProfile() no ha terminado),
  // evitamos enviar "null" al backend y que Spring lance MethodArgumentTypeMismatchException
  if (!profileUserId) return;

  profileIsLoading = true;
  const list = document.getElementById('profilePostsList');

  try {
    const response = await fetch(
      API_URL_PROFILE + '/posts/user/' + profileUserId + '?page=' + page,
      { headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') } }
    );

    if (!response.ok) {
      if (page === 0)
        list.innerHTML = '<div class="empty-state"><p>Error al cargar posts</p></div>';
      return;
    }

    const data = await response.json();

    // Solo limpiar el contenedor en la primera carga
    if (page === 0) {
      list.innerHTML = '';
      profileHasMore = true; // reiniciar por si el usuario cambió de perfil
    }

    // Estado vacío (solo en página 0 y sin resultados)
    if (!data.content || data.content.length === 0 && page === 0) {
      const msg = profileIsOwnProfile
        ? '¡Comparte algo con la comunidad!'
        : 'Este usuario no ha publicado nada todavía.';
      list.innerHTML =
        '<div class="empty-state">' +
          '<div class="empty-state-icon">✏️</div>' +
          '<p class="empty-state-title">Aún no hay posts</p>' +
          '<p class="empty-state-text">' + msg + '</p>' +
        '</div>';
      profileHasMore = false;
      return;
    }

    // Añadir posts al DOM
    data.content.forEach(function(post) {
      const temp = document.createElement('div');
      temp.innerHTML = createPostHTML(post);
      const card = temp.firstElementChild;
      list.appendChild(card);
      attachPostEvents(post.id);
    });

    // Actualizar estado de paginación
    profileCurrentPage = data.number;
    profileHasMore     = !data.last;  // ← clave

  } catch (err) {
    if (page === 0)
      list.innerHTML = '<div class="empty-state"><p>Error al cargar posts</p></div>';
    console.error(err);
  } finally {
    profileIsLoading = false;
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
    window.location.href = './profile.html?user=' + username;
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
    window.location.href = './login.html';
}







// ==========================================
// NAVEGACIÓN + UTILIDADES (reutilizadas de feed.js)
// ==========================================


function showProfileError(msg) {
    document.getElementById('profileDisplayName').textContent = msg;
}







// ==========================================
// SCROLL INFINITO DEL PERFIL
// ==========================================

/**
 * initProfileInfiniteScroll()
 *
 * Observa el #profileSentinel. Cuando entra en el viewport
 * llama a loadUserPostsProfile(profileCurrentPage + 1).
 *
 * Usa las mismas variables de estado (profileIsLoading, profileHasMore)
 * que loadUserPostsProfile para garantizar consistencia.
 *
 * Se llama UNA SOLA VEZ desde DOMContentLoaded. No se recrea
 * cuando cambia de perfil porque el observer ya existe y sigue
 * funcionando; lo que cambia son las variables de estado y profileUserId.
 */
function initProfileInfiniteScroll() {
  const sentinel = document.getElementById('profileSentinel');
  if (!sentinel) return;

  const observer = new IntersectionObserver(
    function(entries) {
      const entry = entries[0];
      if (!entry.isIntersecting) return;

      if (!profileHasMore) {
        observer.disconnect();
        return;
      }

      if (profileIsLoading) return;

      // profileUserId se rellena en loadProfile() antes de que este
      // observer pueda dispararse, así que siempre tendrá valor
      loadUserPostsProfile(profileCurrentPage + 1);
    },
    {
      root:       null,
      rootMargin: '0px 0px 100px 0px',
      threshold:  0.1
    }
  );

  observer.observe(sentinel);
}

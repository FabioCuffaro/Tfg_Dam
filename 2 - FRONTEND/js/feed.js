// ==========================================
// feed.js - Feed conectado al backend real
// ==========================================

const API_URL = 'http://localhost:8080/api';

// ==========================================
// ARRANQUE
// ==========================================

document.addEventListener('DOMContentLoaded', function () {
    // Si no hay token, redirigir al login
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initUserInfo();
    initComposer();
    loadFeed(0);
    initInfiniteScroll() //Nuevo para poder hacer scroll infinto
    initSearch();
    initLogout();
    initLightbox();
});

// ==========================================
// INFO DEL USUARIO EN NAVBAR
// ==========================================

function initUserInfo() {
    const displayName = localStorage.getItem('displayName') || 'Usuario';
    const username    = localStorage.getItem('username') || 'user';
    const initial     = displayName.charAt(0).toUpperCase();

    document.getElementById('navUsername').textContent = '@' + username;
    setAvatar('navAvatar',      initial, null);
    setAvatar('composerAvatar', initial, null);
}

function setAvatar(elementId, initial, avatarUrl) {
    const el = document.getElementById(elementId);
    if (!el) return;
    if (avatarUrl) {
        el.innerHTML = '<img src="' + avatarUrl + '" alt="avatar">';
    } else {
        el.textContent = initial;
    }
}

// ==========================================
// BUSCADOR (conectado al backend)
// ==========================================

function initSearch() {
    const input   = document.getElementById('searchInput');
    const results = document.getElementById('searchResults');
    let debounceTimer;

    input.addEventListener('input', function () {
        const query = input.value.trim();
        clearTimeout(debounceTimer);

        if (query.length < 2) {
            results.classList.remove('visible');
            return;
        }

        debounceTimer = setTimeout(function () { searchUsers(query); }, 300);
    });

    document.addEventListener('click', function (e) {
        if (!e.target.closest('.nav-search')) {
            results.classList.remove('visible');
        }
    });
}

async function searchUsers(query) {
    const results = document.getElementById('searchResults');

    try {
        const response = await fetch(API_URL + '/profiles/search?q=' + encodeURIComponent(query), {
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (!response.ok) return;

        const users = await response.json();

        if (users.length === 0) {
            results.classList.remove('visible');
            return;
        }

        results.innerHTML = users.map(function (u) {
            const initial = (u.displayName || u.username || 'U').charAt(0).toUpperCase();
            return (
                '<div class="search-result-item" onclick="goToProfile(\'' + u.username + '\')">' +
                    '<div class="search-result-avatar">' + initial + '</div>' +
                    '<div>' +
                        '<p class="search-result-name">' + escapeHTML(u.displayName || u.username) + '</p>' +
                        '<p class="search-result-username">@' + escapeHTML(u.username) + '</p>' +
                    '</div>' +
                '</div>'
            );
        }).join('');

        results.classList.add('visible');
    } catch (err) {
        console.error(err);
    }
}

// ==========================================
// LOGOUT
// ==========================================

function initLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function () {
            if (confirm('¿Cerrar sesión?')) logout();
        });
    }
}

function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
}

// ==========================================
// COMPOSER
// ==========================================

// Archivo de imagen seleccionado (null si no hay ninguno)
let selectedImageFile = null;

function initComposer() {
    const textarea   = document.getElementById('postContent');
    const counter    = document.getElementById('charCounter');
    const publishBtn = document.getElementById('publishBtn');
    const attachBtn  = document.getElementById('attachBtn');
    const imageInput = document.getElementById('imageInput');
    const removeBtn  = document.getElementById('removeImageBtn');

    // Actualizar contador y habilitar/deshabilitar botón publicar
    textarea.addEventListener('input', function () {
        const length = textarea.value.length;
        counter.textContent = length + ' / 500';
        counter.className = 'char-counter';
        if (length > 400) counter.className = 'char-counter warning';
        if (length > 470) counter.className = 'char-counter danger';
        // Se puede publicar si hay texto O imagen seleccionada
        publishBtn.disabled = textarea.value.trim().length === 0 && !selectedImageFile;
    });

    // El botón 📎 abre el selector de archivos del sistema
    attachBtn.addEventListener('click', function () {
        imageInput.click();
    });

    // Cuando el usuario elige un archivo, mostrar preview
    imageInput.addEventListener('change', function () {
        const file = imageInput.files[0];
        if (!file) return;

        selectedImageFile = file;
        attachBtn.classList.add('has-image');

        // Mostrar preview con FileReader (sin subir nada todavía)
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('imagePreviewImg').src = e.target.result;
            document.getElementById('imagePreviewContainer').style.display = 'block';
        };
        reader.readAsDataURL(file);

        // Habilitar publicar aunque el textarea esté vacío (imagen sola)
        publishBtn.disabled = false;
    });

    // Botón ✕ del preview → quitar imagen
    removeBtn.addEventListener('click', function () {
        clearImageSelection();
        // Deshabilitar publicar si tampoco hay texto
        publishBtn.disabled = textarea.value.trim().length === 0;
    });

    publishBtn.addEventListener('click', handlePublish);
}

function clearImageSelection() {
    selectedImageFile = null;
    document.getElementById('imageInput').value = '';
    document.getElementById('imagePreviewContainer').style.display = 'none';
    document.getElementById('imagePreviewImg').src = '';
    document.getElementById('attachBtn').classList.remove('has-image');
}

async function handlePublish() {
    const textarea   = document.getElementById('postContent');
    const publishBtn = document.getElementById('publishBtn');
    const btnText    = publishBtn.querySelector('.btn-text');
    const btnLoader  = publishBtn.querySelector('.btn-loader');
    const content    = textarea.value.trim();

    if (!content && !selectedImageFile) return;

    btnText.style.display   = 'none';
    btnLoader.style.display = 'inline';
    publishBtn.disabled     = true;

    try {
        // FormData en lugar de JSON para poder enviar archivo + texto juntos
        // (el navegador pone automáticamente el Content-Type multipart/form-data
        //  con el boundary correcto; si lo ponemos a mano rompemos el boundary)
        const formData = new FormData();
        formData.append('content', content || ' ');   // backend exige content
        if (selectedImageFile) {
            formData.append('image', selectedImageFile);
        }

        const response = await fetch(API_URL + '/posts', {
            method: 'POST',
            headers: {
                // NO añadir Content-Type aquí; el navegador lo gestiona solo
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            body: formData
        });

        if (response.status === 401) { logout(); return; }

        if (response.ok) {
            const newPost = await response.json();
            addPostToTop(newPost);
            textarea.value = '';
            document.getElementById('charCounter').textContent = '0 / 500';
            document.getElementById('charCounter').className = 'char-counter';
            clearImageSelection();
        } else {
            alert('Error al publicar. Inténtalo de nuevo.');
        }
    } catch (err) {
        alert('No se puede conectar con el servidor.');
        console.error(err);
    } finally {
        btnText.style.display   = 'inline';
        btnLoader.style.display = 'none';
        publishBtn.disabled     = true;
    }
}

// ==========================================
// FEED (cargar posts del backend)
// ==========================================

let currentPage = 0;
let isLoading   = false; //Evita lanzar dos fetch a la vez
let hasMore = true; //Nueva variable para ver si hay más paginas para cargar 

async function loadFeed(page) {

  // Guard 1: no lanzar una petición si ya hay otra en vuelo
  if (isLoading) return;

  // Guard 2: no pedir más si el backend ya dijo que es la última página
  if (!hasMore && page !== 0) return;

  isLoading = true;
  const postsList = document.getElementById('postsList');

  try {
    const response = await fetch(API_URL + '/posts?page=' + page, {
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
    });

    if (response.status === 401) { logout(); return; }

    const data = await response.json();

    // Primera página: limpiar skeletons/estado vacío anterior
    if (page === 0) {
      postsList.innerHTML = '';
      // Reiniciar el estado por si el usuario recargó sin recargar la página
      hasMore = true;
    }

    // Estado vacío total (primer fetch, sin resultados)
    if (data.content && data.content.length === 0 && page === 0) {
      postsList.innerHTML =
        '<div class="empty-state">' +
          '<div class="empty-state-icon">📭</div>' +
          '<p class="empty-state-title">El feed está vacío</p>' +
          '<p class="empty-state-text">¡Sé el primero en publicar algo!</p>' +
        '</div>';
      hasMore = false;
      return;
    }

    // Añadir los posts al DOM (sin borrar los ya cargados)
    if (data.content) {
      data.content.forEach(function (post) { addPostToBottom(post); });
    }

    // ── ACTUALIZAR ESTADO DE PAGINACIÓN ─────────────────────────────
    // data.number → página real confirmada por el servidor
    // data.last   → true si ya no hay más páginas
    currentPage = data.number;
    hasMore     = !data.last;   // ← NUEVO

  } catch (err) {
    if (page === 0) {
      postsList.innerHTML = '<div class="empty-state">No se puede comunicar con el servidor, pruebe más tarde </div>';
    }
    console.error(err);
  } finally {
    // Siempre liberar el semáforo, aunque haya error
    isLoading = false;
  }
}

function addPostToTop(post) {
    const postsList = document.getElementById('postsList');
    const emptyState = postsList.querySelector('.empty-state');
    if (emptyState) emptyState.remove();

    const temp = document.createElement('div');
    temp.innerHTML = createPostHTML(post);
    const card = temp.firstElementChild;
    postsList.insertBefore(card, postsList.firstChild);
    attachPostEvents(post.id);
}

function addPostToBottom(post) {
    const postsList = document.getElementById('postsList');
    const temp = document.createElement('div');
    temp.innerHTML = createPostHTML(post);
    const card = temp.firstElementChild;
    postsList.appendChild(card);
    attachPostEvents(post.id);
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
        ? '<img src="' + post.imageUrl + '" alt="imagen" class="post-image" onclick="openLightbox(\'' + post.imageUrl + '\')">'
        : '';

    const deleteBtn = (isOwner || isAdmin)
        ? '<button class="btn-delete-post visible" data-post-id="' + post.id + '">🗑 Eliminar</button>'
        : '';

    return (
        '<div class="post-card" data-post-id="' + post.id + '">' +
            '<div class="post-header">' +
                '<div class="post-avatar" onclick="goToProfile(\'' + post.authorUsername + '\')">' +
                    avatarContent +
                '</div>' +
                '<div>' +
                    '<p class="post-author-name" onclick="goToProfile(\'' + post.authorUsername + '\')">' +
                        escapeHTML(post.authorDisplayName || 'Usuario') +
                    '</p>' +
                    '<p class="post-author-username">@' + escapeHTML(post.authorUsername || '') + '</p>' +
                '</div>' +
                '<span class="post-time">' + formatTime(post.createdAt) + '</span>' +
            '</div>' +
            '<p class="post-content">' + escapeHTML(post.content) + '</p>' +
            imageHTML +
            '<div class="post-footer">' +
                '<button class="btn-like ' + likedClass + '" data-post-id="' + post.id + '">' +
                    heartSVG +
                    '<span class="like-count">' + post.likeCount + '</span>' +
                '</button>' +
                deleteBtn +
            '</div>' +
        '</div>'
    );
}

function attachPostEvents(postId) {
    const likeBtn = document.querySelector('.btn-like[data-post-id="' + postId + '"]');
    if (likeBtn) {
        likeBtn.addEventListener('click', function () { handleLike(postId, likeBtn); });
    }

    const deleteBtn = document.querySelector('.btn-delete-post[data-post-id="' + postId + '"]');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', function () { handleDelete(postId); });
    }
}

// ==========================================
// LIKE (conectado al backend)
// ==========================================

async function handleLike(postId, btn) {
    const isLiked = btn.classList.contains('liked');
    const countEl = btn.querySelector('.like-count');
    const heart   = btn.querySelector('svg');
    const count   = parseInt(countEl.textContent) || 0;

    // Optimistic update
    btn.classList.toggle('liked');
    if (heart) heart.setAttribute('fill', isLiked ? 'none' : 'currentColor');
    countEl.textContent = isLiked ? count - 1 : count + 1;

    try {
        const response = await fetch(API_URL + '/posts/' + postId + '/like', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (response.ok) {
            // Usar el valor real del servidor en lugar del optimista
            const updated = await response.json();
            countEl.textContent = updated.likeCount;
            if (heart) heart.setAttribute('fill', updated.likedByCurrentUser ? 'currentColor' : 'none');
            btn.classList.toggle('liked', updated.likedByCurrentUser);
        } else {
            // Revertir si falla
            btn.classList.toggle('liked');
            if (heart) heart.setAttribute('fill', isLiked ? 'currentColor' : 'none');
            countEl.textContent = count;
        }
    } catch (err) {
        btn.classList.toggle('liked');
        if (heart) heart.setAttribute('fill', isLiked ? 'currentColor' : 'none');
        countEl.textContent = count;
        console.error(err);
    }
}

// ==========================================
// ELIMINAR POST
// ==========================================

async function handleDelete(postId) {
    if (!confirm('¿Seguro que quieres eliminar este post?')) return;

    try {
        const response = await fetch(API_URL + '/posts/' + postId, {
            method: 'DELETE',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        });

        if (response.ok || response.status === 204) {
            const card = document.querySelector('.post-card[data-post-id="' + postId + '"]');
            if (card) card.remove();
        } else {
            alert('Error al eliminar el post.');
        }
    } catch (err) {
        alert('No se puede conectar con el servidor.');
        console.error(err);
    }
}



// ==========================================
// NAVEGACIÓN Y UTILIDADES
// ==========================================

function goToProfile(username) {
    window.location.href = 'profile.html?user=' + username;
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



// ==========================================
// LIGHTBOX (ver imagen completa)
// ==========================================

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


// ==========================================
// SCROLL INFINITO — PEGAR AL FINAL DE feed.js
// ==========================================



// ==========================================
// SCROLL INFINITO DEL FEED
// ==========================================

/**
 * initInfiniteScroll()
 *
 * Crea un IntersectionObserver que vigila el elemento #feedSentinel.
 * Cuando ese elemento entra en el viewport (el usuario llega al final
 * de la lista), comprueba dos condiciones antes de pedir más datos:
 *   1. !isLoading  → no hay ya una petición en vuelo
 *   2.  hasMore    → el backend confirmó que existen más páginas
 *
 * Si ambas se cumplen, llama a loadFeed(currentPage + 1).
 *
 * El observer se desconecta automáticamente cuando hasMore es false
 * (ya no tiene sentido seguir observando si no hay más datos).
 *
 * ¿Por qué threshold: 0.1?
 * Con 0 (valor por defecto) el callback se dispara en cuanto un solo
 * píxel del sentinel es visible. Con 0.1 esperamos a que el 10 % esté
 * visible, lo que reduce falsos positivos en dispositivos lentos donde
 * el scroll puede "rebotar" justo en el borde.
 */
function initInfiniteScroll() {
  const sentinel = document.getElementById('feedSentinel');
  if (!sentinel) return; // prevención por si: si no está el HTML no hacemos nada

  const observer = new IntersectionObserver(
    function(entries) {
      // entries[0] es el centinela (solo observamos uno)
      // isIntersecting = true cuando el elemento entra en la pantalla
      const entry = entries[0];
      if (!entry.isIntersecting) return;

      // Si no hay más páginas, desconectar el observer para liberar recursos
      if (!hasMore) {
        observer.disconnect();
        return;
      }

      // Si hay otra petición en este momento, lo mejor es ignorar este trigger porque le doy prioridad a otras
      if (isLoading) return;

      // ✅ Todo correcto: pedir la siguiente página
      loadFeed(currentPage + 1);
    },
    {
      // root: null   → el viewport del navegador es el área de observación
      // rootMargin   → activar 100px antes de que sea visible (precarga anticipada)
      // threshold    → con que el 10 % sea visible ya es suficiente
      root:       null,
      rootMargin: '0px 0px 100px 0px',
      threshold:  0.1
    }
  );

  // Empezar a observar el centinela
  observer.observe(sentinel);
}
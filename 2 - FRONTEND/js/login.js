        // ==========================================
        // URL BASE DE LA API
        // En desarrollo apunta a localhost.
        // En producción cambia esto por la URL de Render.
        // ==========================================
        const API_URL = 'http://localhost:8080';


        
        // ==========================================
        // SI EL USUARIO YA TIENE TOKEN VÁLIDO, REDIRIGIR AL FEED
        // Evita que alguien que ya hizo login vea la pantalla de login
        // ==========================================
        window.addEventListener('load', () => {
            const token = localStorage.getItem('token');
            if (token) {
                window.location.href = '/feed.html';
            }
        });



        // ==========================================
        // VALIDACIÓN EN TIEMPO REAL
        // Mientras el usuario escribe, limpiamos los errores
        // ==========================================
        document.getElementById('email').addEventListener('input', () => {
            clearFieldError('email');
        });

        document.getElementById('password').addEventListener('input', () => {
            clearFieldError('password');
        });

        // Permitir enviar con Enter
        document.getElementById('password').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleLogin();
        });


        

        // ==========================================
        // FUNCIÓN PRINCIPAL DE LOGIN
        // Esta función conecta el formulario con el backend
        // ==========================================
        async function handleLogin() {
            // 1. Recoger los valores del formulario
            const email    = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;

            // 2. Validar campos vacíos antes de llamar al servidor
            let hasErrors = false;

            if (!email || !isValidEmail(email)) {
                showFieldError('email');
                hasErrors = true;
            }

            if (!password) {
                showFieldError('password');
                hasErrors = true;
            }

            if (hasErrors) return;

            // 3. Mostrar estado de carga
            setLoading(true);
            hideGlobalError();

            try {
                // 4. Llamada al backend con fetch
                // Esto es lo equivalente a lo que hacías en Postman:
                // POST /api/auth/login con body JSON
                const response = await fetch(`${API_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ email, password })
                });

                // 5. Leer la respuesta como JSON
                const data = await response.json();




                if (response.ok) {
                    // ----------------------- LOGIN EXITOSO -------------------------------
                    // Guardamos en localStorage todo lo que el backend nos devuelve.
                    // El token es lo más importante: sin él no podemos hacer
                    // peticiones autenticadas al backend.
                    localStorage.setItem('token',       data.token);
                    localStorage.setItem('tokenType',   data.tokenType);
                    localStorage.setItem('userId',      data.userId);
                    localStorage.setItem('email',       data.email);
                    localStorage.setItem('username',    data.username);
                    localStorage.setItem('displayName', data.displayName);
                    localStorage.setItem('role',        data.role);

                    // Redirigir al feed
                    window.location.href = '/feed.html';

                } else {
                    // --------------- ERROR DEL SERVIDOR -------------------------
                    // // 401 = credenciales incorrectas
                    // 400 = datos inválidos
                    // 500 = error interno del servidor
                    if (response.status === 401) {
                        showGlobalError('Email o contraseña incorrectos');
                    } else {
                        showGlobalError('Error al iniciar sesión. Inténtalo de nuevo.');
                    }
                }


            } catch (error) {
                // Error de red: el servidor no responde (está apagado, etc.)
                showGlobalError('No se puede conectar con el servidor. Comprueba tu conexión.');
                console.error('Error de red:', error);

            } finally {
                // Siempre ocultamos la barra de carga, haya ido bien o mal
                setLoading(false);
            }
        }




        
        // ==========================================
        // FUNCIONES DE UTILIDAD DE LA INTERFAZ
        // ==========================================

        function isValidEmail(email) {
            return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        }

        function showFieldError(fieldId) {
            document.getElementById(fieldId).classList.add('error');
            document.getElementById(fieldId + 'Error').classList.add('visible');
        }

        function clearFieldError(fieldId) {
            document.getElementById(fieldId).classList.remove('error');
            document.getElementById(fieldId + 'Error').classList.remove('visible');
        }

        function showGlobalError(message) {
            const el = document.getElementById('globalError');
            el.textContent = message;
            el.classList.add('error');
        }

        function hideGlobalError() {
            const el = document.getElementById('globalError');
            el.classList.remove('error');
        }

        function setLoading(isLoading) {
            const btn        = document.getElementById('loginBtn');
            const loadingBar = document.getElementById('loadingBar');

            if (isLoading) {
                btn.disabled     = true;
                btn.textContent  = 'Iniciando sesión...';
                loadingBar.classList.add('visible');
            } else {
                btn.disabled     = false;
                btn.textContent  = 'Iniciar Sesión';
                loadingBar.classList.remove('visible');
            }
        }
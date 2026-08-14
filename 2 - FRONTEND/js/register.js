 const API_URL = 'http://localhost:8080';

        // Si ya está logueado, redirigir al feed directamente
        window.addEventListener('load', () => {
            if (localStorage.getItem('token')) {
                window.location.href = './feed.html';
            }
        });



        // ==========================================
        // VALIDACIÓN EN TIEMPO REAL mientras el usuario escribe
        // ==========================================

        document.getElementById('username').addEventListener('input', function() {
            clearFieldError('username');
        });

        document.getElementById('displayName').addEventListener('input', function() {
            clearFieldError('displayName');
        });

        document.getElementById('email').addEventListener('input', function() {
            clearFieldError('email');
        });

        document.getElementById('password').addEventListener('input', function() {
            clearFieldError('password');
            updatePasswordStrength(this.value);
        });

        document.getElementById('confirmPassword').addEventListener('input', function() {
            clearFieldError('confirmPassword');
        });

        // Enviar con Enter desde el último campo
        document.getElementById('confirmPassword').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleRegister();
        });




        // ==========================================
        // FUNCIÓN PRINCIPAL DE REGISTRO
        // Conecta el formulario con POST /api/auth/register
        // ==========================================
        async function handleRegister() {
            // 1. Recoger valores del formulario
            const username        = document.getElementById('username').value.trim();
            const displayName     = document.getElementById('displayName').value.trim();
            const email           = document.getElementById('email').value.trim();
            const password        = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            // 2. Validaciones en el frontend antes de llamar al servidor
            let hasErrors = false;

            if (!username || !/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
                showFieldError('username', 'Solo letras, números y guion bajo (3-20 caracteres)');
                hasErrors = true;
            }

            if (!displayName) {
                showFieldError('displayName', 'El nombre es obligatorio');
                hasErrors = true;
            }

            if (!email || !isValidEmail(email)) {
                showFieldError('email', 'Introduce un email válido');
                hasErrors = true;
            }

            if (!password || password.length < 6) {
                showFieldError('password', 'Mínimo 6 caracteres');
                hasErrors = true;
            }

            if (password !== confirmPassword) {
                showFieldError('confirmPassword', 'Las contraseñas no coinciden');
                hasErrors = true;
            }

            if (hasErrors) return;

            // 3. Mostrar carga
            setLoading(true);
            hideAlert();

            try {
                // 4. Llamada al backend: POST /api/auth/register
                // Enviamos exactamente los campos que espera RegisterRequest.java
                const response = await fetch(`${API_URL}/api/auth/register`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        username,
                        displayName,
                        email,
                        password
                    })
                });

                const data = await response.json();

                if (response.ok) {
                    // --------------- REGISTRO EXITOSO -------------
                    // Igual que en login: guardamos todo en localStorage
                    localStorage.setItem('token',       data.token);
                    localStorage.setItem('tokenType',   data.tokenType);
                    localStorage.setItem('userId',      data.userId);
                    localStorage.setItem('email',       data.email);
                    localStorage.setItem('username',    data.username);
                    localStorage.setItem('displayName', data.displayName);
                    localStorage.setItem('role',        data.role);

                    // Mostramos mensaje de éxito brevemente antes de redirigir
                    showAlert('¡Cuenta creada! Redirigiendo...', 'success');

                    // Redirigir al feed tras un pequeño delay para que vea el mensaje
                    setTimeout(() => {
                        window.location.href = './feed.html';
                    }, 3000);

                } else {
                    // ------- ERROR DEL SERVIDOR ------------
                    // El backend puede devolver mensajes específicos:
                    // "El email ya está registrado" o "El username ya está en uso"
                    const errorMessage = data.message || 'Error al crear la cuenta';
                    showAlert(errorMessage, 'error');
                }

            } catch (error) {
                showAlert('No se puede conectar con el servidor. Comprueba tu conexión.', 'error');
                console.error('Error de red:', error);

            } finally {
                setLoading(false);
            }
        }




        // ==========================================
        // INDICADOR DE DEBILIDAD DE CONTRASEÑA
        // Muestra visualmente si la contraseña es débil, moderada o fuerte mientras el usuario escribe
        // ==========================================
        function updatePasswordStrength(password) {
            const strengthEl   = document.getElementById('passwordStrength');
            const fillEl       = document.getElementById('strengthFill');
            const textEl       = document.getElementById('strengthText');

            if (!password) {
                strengthEl.classList.remove('visible');
                return;
            }

            strengthEl.classList.add('visible');

            // Calcular puntuación de fortaleza
            let score = 0;
            if (password.length >= 6)  score++;
            if (password.length >= 10) score++;
            if (/[A-Z]/.test(password)) score++;
            if (/[0-9]/.test(password)) score++;
            if (/[^A-Za-z0-9]/.test(password)) score++;

            const levels = [
                { width: '20%',  color: '#e74c3c', text: 'Muy débil' },
                { width: '40%',  color: '#e67e22', text: 'Débil' },
                { width: '60%',  color: '#f1c40f', text: 'Moderada' },
                { width: '80%',  color: '#2ecc71', text: 'Fuerte' },
                { width: '100%', color: '#27ae60', text: 'Muy fuerte' },
            ];

            const level = levels[Math.min(score - 1, 4)] || levels[0];
            fillEl.style.width      = level.width;
            fillEl.style.background = level.color;
            textEl.textContent      = `Seguridad: ${level.text}`;
        }






        // ==========================================
        // FUNCIONES DE UTILIDAD
        // ==========================================

        function isValidEmail(email) {
            return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        }

        function showFieldError(fieldId, message) {
            const input = document.getElementById(fieldId);
            const error = document.getElementById(fieldId + 'Error');
            input.classList.add('error');
            if (message) error.textContent = message;
            error.classList.add('visible');
        }

        function clearFieldError(fieldId) {
            document.getElementById(fieldId).classList.remove('error');
            document.getElementById(fieldId + 'Error').classList.remove('visible');
        }

        function showAlert(message, type) {
            const el = document.getElementById('globalAlert');
            el.textContent = message;
            el.className = `alert ${type}`;
        }

        function hideAlert() {
            document.getElementById('globalAlert').className = 'alert';
        }

        function setLoading(isLoading) {
            const btn        = document.getElementById('registerBtn');
            const loadingBar = document.getElementById('loadingBar');

            if (isLoading) {
                btn.disabled    = true;
                btn.textContent = 'Creando cuenta...';
                loadingBar.classList.add('visible');
            } else {
                btn.disabled    = false;
                btn.textContent = 'Crear Cuenta';
                loadingBar.classList.remove('visible');
            }
        }

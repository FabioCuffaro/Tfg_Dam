// MANEJANDO EL REGISTRO

    document.getElementById('registerForm').addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const username = document.getElementById('username').value;
      const displayName = document.getElementById('displayName').value;
      const email = document.getElementById('email').value;
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirmPassword').value;
      const errorDiv = document.getElementById('errorMessage');
      const registerBtn = document.getElementById('registerBtn');


      // 1. Validar longitud (Mínimo 6 caracteres)
    if (password.length < 6) {
    errorDiv.textContent = 'La contraseña debe tener al menos 6 caracteres';
    errorDiv.style.display = 'block';
    return; // Detiene la ejecución
}
      
        // 2. Validar contraseñas
      if (password !== confirmPassword) {
        errorDiv.textContent = 'Las contraseñas no coinciden';
        errorDiv.style.display = 'block';
        return;
      }
      
      // 3. Mostrar loader
      registerBtn.querySelector('.btn-text').style.display = 'none';
      registerBtn.querySelector('.btn-loader').style.display = 'inline';
      registerBtn.disabled = true;
      errorDiv.style.display = 'none';
      
      
    });

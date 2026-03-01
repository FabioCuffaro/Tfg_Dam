
    // MANEJANDO EL LOGIN

    //Cojo con GetElementByID el formulario de login y le digo que hay un evento al pulsar el boton submit

    document.getElementById('loginForm').addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const email = document.getElementById('email').value;
      const password = document.getElementById('password').value;
      const errorDiv = document.getElementById('errorMessage');
      const loginBtn = document.getElementById('loginBtn');
      
      // Mostrar loader
      loginBtn.querySelector('.btn-text').style.display = 'none';
      loginBtn.querySelector('.btn-loader').style.display = 'inline';
      loginBtn.disabled = true;
      errorDiv.style.display = 'none';

    });
      
      
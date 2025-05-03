import { useState, useEffect } from 'react';
import { createClient } from '@supabase/supabase-js';

// Crear el cliente de Supabase usando variables de entorno
const supabase = createClient(
  process.env.SUPABASE_URL || '',
  process.env.SUPABASE_ANON_KEY || ''
);

// print supabase
console.log("supabase", supabase);

const ResetPassword = () => {
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const [accessToken, setAccessToken] = useState<string | null>(null);

  useEffect(() => {
    // Obtener el access_token del fragmento de la URL (hash)
    console.log("Intentando obtener el access_token de la URL");
    console.log("window.location.hash", window.location.hash);
    console.log("window.location", window.location);

    const hash = window.location.hash;
    const token = new URLSearchParams(hash.substring(1)).get('access_token');
    
    if (token) {
      console.log("Token encontrado en el hash de la URL", token);
      setAccessToken(token);
    } else {
      setMessage('No access token found in URL');
      console.log("No access token found in URL");
    }
  }, []);
  

  // Manejar el envío del formulario para actualizar la contraseña
  const handleReset = async () => {
    if (!accessToken) {
      setMessage('No access token in URL');
      return;
    }
    if (!password) {
      setMessage('Please enter a new password');
      return;
    }

    setLoading(true);

    try {
      const { error } = await supabase.auth.updateUser({
        password,
      });

      if (error) {
        setMessage(`Error: ${error.message}`);
      } else {
        setMessage('Password updated successfully!');
      }
    } catch (error: any) {
      setMessage(`Error: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 40, maxWidth: 400, margin: '0 auto' }}>
      <h1>Reset Your Password</h1>

      <input
        type="password"
        placeholder="New password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        style={{ display: 'block', marginBottom: 10, padding: 8, width: '100%' }}
        disabled={loading}
      />
      <button
        onClick={handleReset}
        style={{ padding: 10, width: '100%', backgroundColor: '#007BFF', color: '#fff', border: 'none' }}
        disabled={loading}
      >
        {loading ? 'Updating...' : 'Update Password'}
      </button>

      {message && <p>{message}</p>}
    </div>
  );
};

export default ResetPassword;

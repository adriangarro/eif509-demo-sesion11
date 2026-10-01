import { useState } from "react";
import { api } from "./api.js";
import { guardarToken } from "./sesion.js";

// Pantalla de inicio de sesión contra POST /auth/login.
export default function Login({ alIniciarSesion }) {
  const [correo, setCorreo] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      const respuesta = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ correo, clave }),
      });
      guardarToken(respuesta.token);
      alIniciarSesion(respuesta.token);
    } catch (problema) {
      setError(problema);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="angosto">
      <h1>Iniciar sesión</h1>
      {error && <p className="error">{error.detail ?? error.title}</p>}
      <form onSubmit={enviar} className="formulario">
        <label>
          Correo
          <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)}
                 autoComplete="username" required autoFocus />
        </label>
        <label>
          Clave
          <input type="password" value={clave} onChange={(e) => setClave(e.target.value)}
                 autoComplete="current-password" required />
        </label>
        <button type="submit" disabled={enviando}>{enviando ? "Ingresando..." : "Entrar"}</button>
      </form>
      <p className="ayuda">
        Usuarios de demostración: <code>admin@demo.cr</code> / <code>admin123</code> (ADMIN) y{" "}
        <code>cliente@demo.cr</code> / <code>cliente123</code> (CLIENTE).
      </p>
    </main>
  );
}

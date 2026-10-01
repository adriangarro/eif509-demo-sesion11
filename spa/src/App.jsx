import { useState } from "react";
import Login from "./Login.jsx";
import ProductoLista from "./ProductoLista.jsx";
import ProductoNuevo from "./ProductoNuevo.jsx";
import { borrarToken, datosDelToken, leerToken } from "./sesion.js";

// Componente raíz. Si no hay token, muestra el inicio de sesión; si lo
// hay, el catálogo. (Una aplicación más grande usaría un enrutador, como
// React Router; aquí basta con el estado.)
export default function App() {
  const [token, setToken] = useState(leerToken());
  const [recarga, setRecarga] = useState(0);

  if (!token) {
    // Si la API redirigió a /login por un 401, se limpia la ruta.
    if (window.location.pathname !== "/") window.history.replaceState(null, "", "/");
    return <Login alIniciarSesion={setToken} />;
  }

  const usuario = datosDelToken(token);

  function cerrarSesion() {
    // Con JWT, "cerrar sesión" es descartar el token en el cliente: el
    // token sigue siendo válido hasta que expire, por eso su vigencia es corta.
    borrarToken();
    setToken(null);
  }

  return (
    <>
      <header className="menu">
        <span className="marca">EIF509 · Catálogo (SPA en React)</span>
        <span className="usuario">
          {usuario?.correo} ({usuario?.roles.join(", ")})
          <button onClick={cerrarSesion}>Cerrar sesión</button>
        </span>
      </header>
      <main>
        <h1>Productos</h1>
        <ProductoLista recarga={recarga} />
        <ProductoNuevo alCrear={() => setRecarga(recarga + 1)} />
      </main>
    </>
  );
}

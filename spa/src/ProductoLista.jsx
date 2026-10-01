import { useEffect, useState } from "react";
import { api } from "./api.js";

const TAMANO = 5;

// Listado paginado con GET /api/v1/productos. Toda consulta pasa por tres
// estados que la pantalla debe mostrar: cargando, datos y error.
export default function ProductoLista({ recarga }) {
  const [numero, setNumero] = useState(0);
  // El resultado guarda a qué consulta corresponde: mientras no coincida
  // con la consulta actual, la pantalla está «cargando».
  const [resultado, setResultado] = useState(null);
  const consulta = `/api/v1/productos?page=${numero}&size=${TAMANO}#${recarga}`;

  // El efecto se ejecuta al mostrar el componente y cada vez que cambia la
  // consulta (otra página o una recarga después de crear un producto).
  useEffect(() => {
    let vigente = true;
    api(consulta.split("#")[0])
      .then((pagina) => vigente && setResultado({ consulta, pagina }))
      .catch((error) => vigente && setResultado({ consulta, error }));
    return () => {
      vigente = false;   // evita actualizar el estado con una respuesta vieja
    };
  }, [consulta]);

  if (resultado?.consulta !== consulta) return <p className="cargando">Cargando...</p>;
  const { pagina, error } = resultado;
  if (error) return <p className="error">{error.detail ?? error.title}</p>;

  const { totalElements, totalPages } = pagina.page;
  return (
    <section>
      <p>Productos en el catálogo: {totalElements}</p>
      <table>
        <thead>
          <tr><th>Nombre</th><th className="numero">Precio (₡)</th><th className="numero">Existencias</th></tr>
        </thead>
        <tbody>
          {pagina.content.map((p) => (
            // React escapa el texto por defecto: un nombre con <script> se
            // muestra como texto, igual que th:text en la Sesión 10.
            <tr key={p.id}>
              <td>{p.nombre}</td>
              <td className="numero">{Number(p.precio).toLocaleString("es-CR", { minimumFractionDigits: 2 })}</td>
              <td className="numero">{p.disponible}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <nav className="paginacion">
        <button onClick={() => setNumero(numero - 1)} disabled={numero === 0}>« Anterior</button>
        <span>Página {numero + 1} de {Math.max(totalPages, 1)}</span>
        <button onClick={() => setNumero(numero + 1)} disabled={numero + 1 >= totalPages}>Siguiente »</button>
      </nav>
    </section>
  );
}

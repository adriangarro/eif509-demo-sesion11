// El token se guarda en localStorage: es sencillo y sobrevive a las
// recargas, pero cualquier script de la página puede leerlo (riesgo de
// XSS). Es aceptable en el curso porque React escapa el contenido y el
// token vence en una hora; la decisión debe documentarse en un ADR.
const CLAVE = "token";

export function leerToken() {
  return localStorage.getItem(CLAVE);
}

export function guardarToken(token) {
  localStorage.setItem(CLAVE, token);
}

export function borrarToken() {
  localStorage.removeItem(CLAVE);
}

// Lee el contenido (payload) del token para mostrar el usuario y su rol.
// No verifica la firma: eso lo hace la API en cada petición. Cualquiera
// puede leer el contenido de un JWT; lo que no puede es modificarlo.
export function datosDelToken(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = new TextDecoder().decode(Uint8Array.from(atob(payload), (c) => c.charCodeAt(0)));
    const datos = JSON.parse(json);
    return { correo: datos.sub, roles: datos.roles ?? [], expira: new Date(datos.exp * 1000) };
  } catch {
    return null;
  }
}

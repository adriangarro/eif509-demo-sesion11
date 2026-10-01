// api.js: un solo lugar para hablar con la API.
// Todas las llamadas pasan por esta función: agrega el token y, si la API
// responde 401 con un token vencido o inválido, regresa al inicio de
// sesión. Así no se repite esta lógica en cada pantalla.
import { borrarToken, leerToken } from "./sesion.js";

// Dirección de la API. Se puede cambiar con la variable VITE_API_URL
// (archivo .env.local); por defecto, la API local de Spring Boot.
export const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function api(ruta, opciones = {}) {
  const token = leerToken();
  let resp;
  try {
    resp = await fetch(API_URL + ruta, {
      ...opciones,
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
        ...opciones.headers,
      },
    });
  } catch {
    // fetch solo falla así cuando no hay respuesta legible: la API no está
    // en ejecución, o el navegador la bloqueó por CORS (ver la consola).
    throw {
      title: "Sin conexión",
      detail: "No se pudo conectar con la API. Verifiquen que esté en ejecución y que permita este origen (CORS).",
    };
  }

  // 401 con token: venció o no es válido. Se descarta y se vuelve al
  // inicio de sesión. (El 401 de un inicio de sesión fallido no lleva
  // token y se informa como cualquier otro error.)
  if (resp.status === 401 && token) {
    borrarToken();
    window.location.href = "/login";
    throw { title: "Sesión vencida", detail: "Inicien sesión de nuevo." };
  }

  if (!resp.ok) throw await leerProblema(resp);   // Problem Details (RFC 9457)
  if (resp.status === 204) return null;
  return resp.json();
}

// Los errores de la API vienen en formato Problem Details desde la Sesión 9.
async function leerProblema(resp) {
  try {
    return await resp.json();
  } catch {
    return { title: `Error ${resp.status}`, detail: resp.statusText };
  }
}

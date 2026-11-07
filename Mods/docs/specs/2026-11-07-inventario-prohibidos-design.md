# Diseño: Revisar inventario y Objetos prohibidos — Senju Core, Forge 1.20.1

Fecha: 2026-11-07
Estado: borrador para revisión

## 1. Intención

Fase 2 del Panel de Staff. El staff puede (a) **revisar el inventario** de cualquier jugador
conectado y (b) mantener una **lista de objetos prohibidos** que el servidor **quita
automáticamente** a los jugadores normales, avisando al staff.

## 2. Decisiones del usuario

- La lista se arma **dentro del juego**, en un submenú tipo cofre donde el staff coloca los ítems
  prohibidos. Se probará con el **diamante**.
- Si un jugador normal tiene un ítem prohibido: **desaparece de su inventario y el staff recibe un aviso**.
- (De fases anteriores) Menú tipo cofre, todo en el servidor; staff = operadores + lista `staff`.

## 3. Comportamiento

### 3.1 Panel de Staff (cambios)
- El botón **Revisar inventario** (antes "Próximamente") abre el **selector de jugadores**.
- Nuevo botón **Objetos prohibidos** (barrera roja / libro) en la fila 3 → abre el submenú de la lista.
  *(mi decisión: posición fila 3, col 2; "Cerrar" sigue en fila 3, col 5)*

### 3.2 Selector de jugadores
- Ventana de 6 filas: hasta 45 cabezas de jugadores conectados (con su skin y nombre), ordenadas
  por nombre; fila inferior: Volver, Página anterior, Página siguiente.
- Muestra a todos los jugadores conectados, incluido el propio staff *(mi decisión)*.
- Clic en una cabeza → abre su inventario.

### 3.3 Ver inventario (solo mirar)
- Ventana de 6 filas titulada "Inventario de <nombre>":
  - Filas 1–3: inventario principal; fila 4: barra rápida; fila 5: armadura (4) + mano secundaria;
    fila 6: Volver, **Cofre de ender**, Actualizar.
- Se ve el contenido **en vivo** (se refresca cada segundo) *(mi decisión)*.
- **Solo mirar:** no se puede sacar, meter ni mover nada (mismo bloqueo que el Panel de Staff).
- Los ítems prohibidos aparecen con una línea roja "⚠ Objeto prohibido" en su descripción.
  (En la práctica un no staff nunca los conserva más de 1 s; sirve sobre todo al revisar a otro staff.)
- **Cofre de ender**: misma vista, 3 filas, con Volver.
- Si el jugador se desconecta, la ventana vuelve al selector.

### 3.4 Lista de objetos prohibidos (submenú)
- Ventana de 6 filas "Objetos prohibidos": 45 casillas para la lista + fila inferior (Volver,
  páginas). Muestra un ítem de cada tipo prohibido.
- **Agregar:** el staff toma un ítem de su inventario y hace clic en una casilla vacía (o
  shift‑clic desde su inventario). Se agrega **el tipo** de ítem a la lista; **el ítem del staff
  no se pierde** (vuelve a su inventario/cursor).
- **Quitar:** clic sobre un ítem de la lista → se elimina de la lista.
- Comparación **solo por tipo** (cualquier diamante, sin importar cantidad, nombre o encantamientos).
- La Placa de Staff no se puede agregar a la lista *(mi decisión: evitaría que el staff la pierda… aunque el staff está exento, se bloquea para evitar confusión)*.
- Guardada en el mundo (SavedData); compartida por todo el staff; sobrevive a reinicios.
- Al agregar/quitar: aviso a todo el staff conectado: "<staff> prohibió: Diamante" / "<staff> permitió: Diamante".

### 3.5 Confiscación automática
- Aplica a **jugadores que no son staff** (el staff está exento).
- Se revisa cada segundo y al recoger un ítem: inventario principal, armadura, mano secundaria,
  cursor y cofre de ender. *(mi decisión: incluir el cofre de ender para que no sea un escondite)*
- Los ítems prohibidos **se eliminan** (no caen al suelo).
- **Al jugador:** mensaje "Se te quitó un objeto prohibido: Diamante x3".
- **Al staff conectado:** aviso "Se confiscó a <jugador>: Diamante x3".
- **Registro:** cada confiscación se anota en `logs/senjucore-confiscados.log` con fecha, jugador,
  ítem y cantidad *(mi decisión: si alguna vez hay un error en la lista, queda constancia de qué se borró)*.
- No revisa cofres del mundo, shulkers ni contenedores de bloques *(no‑objetivo de esta fase)*.

## 4. Objetivos y anti‑objetivos

**Objetivos:** que un jugador normal no pueda conservar un objeto prohibido; que el staff sepa
cuándo pasa; revisar inventarios sin poder dañarlos por accidente.

**Anti‑objetivos:** que el staff pierda ítems al revisar o al armar la lista; que un ítem
prohibido caiga al suelo; que un no staff pueda abrir estas ventanas o modificar la lista.

## 5. No‑objetivos (por ahora)

- Quitar o devolver ítems manualmente desde la vista de inventario.
- Revisar cofres del mundo, shulkers dentro del inventario (contenido anidado) o inventarios de
  jugadores desconectados.
- Comparar por NBT/encantamientos (ítems con encantamientos imposibles).
- Castigar (fase 3).

## 6. Criterios de terminado

1. El Panel de Staff tiene Revisar inventario (funcional) y Objetos prohibidos.
2. El selector muestra a los jugadores conectados con páginas.
3. La vista de inventario (y de cofre de ender) muestra el contenido en vivo y no permite mover nada.
4. Se puede agregar el diamante a la lista sin perder el diamante del staff, y quitarlo con un clic.
5. Un jugador normal con diamantes los pierde en ≤ 1 s, recibe el mensaje, el staff recibe el
   aviso y queda una línea en `senjucore-confiscados.log`.
6. El staff con diamantes no los pierde.
7. `gradlew build test` pasa; la lógica pura tiene pruebas.

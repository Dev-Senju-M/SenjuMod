# Diseño: Menú del Staff y Vanish — Senju Core, Forge 1.20.1

Fecha: 2026-11-06
Estado: borrador para revisión

## 1. Intención

La Placa de Staff abre un **menú tipo cofre** con las herramientas del staff. Esta fase
construye el menú y la primera función, **Vanish** (volverse invisible para los jugadores).
"Castigar" y "Revisar inventario" aparecen como botones que todavía dicen "Próximamente"; se
diseñan en las fases siguientes.

## 2. Decisiones del usuario

- Menú **tipo cofre**, toda la lógica en el servidor (propuesto por mí, aceptado).
- Orden de fases: 1) menú + Vanish, 2) Revisar inventario + objetos prohibidos, 3) Castigar.
- Vanish (aceptado tal como lo propuse):
  - Los jugadores normales no te ven, no apareces en TAB y no ven "se unió"/"salió" si entras o
    sales estando en Vanish.
  - Los monstruos no te atacan; no haces ruido (pasos, abrir cofres).
  - Tienes un aviso permanente en pantalla mientras estás en Vanish.

## 3. Comportamiento

### 3.1 Menú del staff
- Clic derecho con la placa (solo staff) abre "Panel de Staff", 3 filas, fondo de cristal gris.
- Botones (ítems con nombre y descripción):
  - Fila 2, col 2: **Castigar** (mazo/hacha) → "Próximamente".
  - Fila 2, col 5: **Vanish** (poción de invisibilidad): nombre "Vanish: activado/desactivado";
    brillo cuando está activo. Clic → alterna Vanish y actualiza el botón sin cerrar el menú.
  - Fila 2, col 8: **Revisar inventario** (cofre) → "Próximamente".
  - Fila 3, col 5: **Cerrar** (barrera).
- No se puede sacar ni meter nada en el menú (shift‑clic, arrastrar, teclas numéricas, Q,
  doble clic: todo bloqueado). Las placas siguen protegidas igual.
- Si alguien deja de ser staff con el menú abierto, se le cierra.

### 3.2 Vanish
- Se alterna desde el menú o con `/staff vanish` (solo staff).
- Al activar: mensaje "Vanish activado" y aviso a los demás staff: "<nombre> activó Vanish".
  Igual al desactivar.
- **Para jugadores normales** (no staff):
  - No reciben tu entidad (no te ven, no ven tu armadura, ni partículas, ni oyen tus pasos).
  - No apareces en la lista TAB.
  - No ven "<nombre> se unió/abandonó la partida" si entras o sales en Vanish.
- **Para el staff:** te ven normal *(mi decisión: simplifica mucho; ver §5)*.
- **Monstruos:** no te eligen como objetivo y, si ya te perseguían, te pierden.
- **Silencio:** abrir cofres, barriles y shulkers no hace sonido ni animación; no recoges ítems
  del suelo mientras estás en Vanish *(mi decisión: recoger un ítem lo haría desaparecer a la vista de todos)*.
- **Persistencia:** el estado se guarda en el mundo; si sales y entras, o se reinicia el servidor,
  sigues en Vanish.
- **Aviso en pantalla:** la primera línea del panel HUD muestra "VANISH" (morado, parpadeando
  suave) mientras estás en Vanish. *(mi decisión: reutiliza el panel existente)*
- Si dejas de ser staff estando en Vanish, se desactiva automáticamente.

## 4. Objetivos y anti‑objetivos

**Objetivos:** que un jugador normal no pueda saber que un staff en Vanish está cerca ni
conectado; que el staff siempre sepa si está en Vanish.

**Anti‑objetivos:** que un jugador normal te vea, te encuentre en TAB, vea tu mensaje de entrada,
oiga un cofre abrirse o vea desaparecer un ítem; que un no staff pueda activar Vanish; que el
staff olvide que está en Vanish.

## 5. No‑objetivos (por ahora)

- Que el staff vea a otros staff semitransparentes (requiere render especial en el cliente).
- Ocultar efectos físicos indirectos: placas de presión, cables trampa, bloques que rompas,
  sonidos de bloques que coloques.
- Ocultarte de comandos con selectores (`@a`, `/list` de operadores).
- Selector de jugadores, Castigar, Revisar inventario (fases 2 y 3).

## 6. Arquitectura (guía para el plan)

```
com.senju.core.staff.menu/
  MenuStaff.java        // AbstractContainerMenu propio sobre un SimpleContainer de 27, bloquea movimientos
  BotonesMenu.java      // construye los ItemStack de los botones (nombre, descripción, brillo)
com.senju.core.staff.vanish/
  RegistroVanish.java   // SavedData: Set<UUID>
  Vanish.java           // activar/desactivar, esVanish(UUID), ocultar/mostrar a cada jugador
  EventosVanish.java    // join/leave, objetivo de mobs, recoger ítems, abrir contenedores en silencio
  mixin/…               // donde Forge no ofrezca evento: no enviar la entidad a no staff,
                        // no enviar info de TAB, suprimir mensajes de entrada/salida
network/PaqueteVanish   // S2C: estás en Vanish sí/no → HUD
```
Lógica pura con pruebas: "¿este espectador debe ver a este jugador?" (vanish, esStaff del espectador).

## 7. Criterios de terminado

1. Clic derecho con la placa abre el Panel de Staff; nada se puede sacar ni meter.
2. Vanish se alterna desde el menú y con `/staff vanish`; el botón refleja el estado.
3. Con dos jugadores (uno no staff): el no staff no te ve, no te ve en TAB, no ve tu entrada/salida.
4. Un monstruo no te ataca en Vanish.
5. Abrir un cofre en Vanish no suena ni se anima para el otro jugador; no recoges ítems.
6. El HUD muestra "VANISH" mientras está activo; sobrevive a reconectar y a reiniciar el servidor.
7. `gradlew build test` pasa; la lógica pura tiene pruebas.

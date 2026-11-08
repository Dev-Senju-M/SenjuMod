# Diseño: Misiones — Senju Core, Forge 1.20.1

Fecha: 2026-11-08
Estado: borrador para revisión

## 1. Intención

La serie usa muchos mods, así que el staff crea las misiones **dentro del juego** colocando
ítems (de cualquier mod) en un menú, igual que la lista de objetos prohibidos. El jugador debe
conseguir **todo con sus propias manos**: minado por él, fundido por él, cocinado por él,
crafteado por él. Cuando tiene la misión completa en el inventario, se le recoge
automáticamente y recibe la recompensa. Cada día hay una **misión global obligatoria**: quien no
la cumple es castigado.

## 2. Decisiones del usuario

- Las misiones se crean en un **menú tipo cofre** desde la Placa de Staff: se colocan los ítems a
  conseguir y su cantidad, y en el mismo menú los **ítems de recompensa**. Cada misión tiene
  **nombre**.
- Objetivos: conseguir ítems ("picar 64 de piedra" = entregar 64 de roca minada por él).
- **Todo debe conseguirlo el propio jugador** (lista de §3.4, aprobada).
- Al tener toda la misión en el inventario: **se le quitan los ítems automáticamente y se le da la
  recompensa**.
- Dos tipos: **Global** (una sola activa por jugador, es la misión del día) y **Personal**
  (asignada a jugadores concretos; puede tener varias).
- La global cambia a **medianoche (hora real)**; se elige de una **cola en orden** preparada por
  el staff (día 1, día 2…). Cola vacía = ese día no hay global ni castigo.
- **Incumplir la global = castigo (opción C):** castigo automático por defecto elegido al crear la
  misión + lista de incumplidores para el staff, que puede perdonar o añadir castigo.
- Quien **no se conecta** ese día también es castigado, salvo que tenga **permiso de ausencia**.
- Los **eliminados** (sin vidas) quedan exentos hasta que los revivan.
- El jugador ve sus misiones en el **panel (HUD)**, en un **menú (tecla J / `/misiones`)** y con
  **avisos**.

## 3. Comportamiento

### 3.1 Crear y editar misiones (staff)
Placa de Staff → botón **Misiones** (antes "Próximamente") → **Gestor de misiones** (6 filas):
- **Cola global:** las misiones globales en orden (la de hoy marcada con brillo y "HOY"). Clic =
  editar; botones para subir/bajar en la cola y eliminar (con confirmación).
- **Personales activas:** misiones personales y a quién están asignadas.
- **Crear misión**, **Incumplidores de ayer** (§3.6), **Permisos de ausencia** (§3.7).

**Menú de creación/edición** (6 filas):
- Filas 1–2: **Objetivos** (hasta 18 ítems). Colocar un stack con el cursor añade ese ítem con su
  cantidad (sin consumirlo, como en Prohibidos); shift‑clic desde el inventario también.
  Sobre un objetivo: clic izq. **+1**, clic der. **−1**, shift+izq. **+16**, shift+der. **−16**
  (mínimo 1, máximo 9999); cantidad en la descripción. Botón soltar (Q) sobre un objetivo lo quita.
  El ítem se compara por **tipo** (no NBT), como los prohibidos.
- Fila 3: separador "Recompensa".
- Fila 4: **Recompensa** (hasta 9 stacks). Se guardan **copias exactas** (con NBT: encantamientos,
  nombre); el staff no pierde los ítems. Mismo control de cantidades.
- Fila 6, botones:
  - **Nombre** — cierra el menú, el staff escribe el nombre en el chat (no se publica; "cancelar"
    lo anula) y el menú se reabre. Máx. 32 caracteres.
  - **Tipo**: Global / Personal (alterna).
  - **Castigo por defecto** (solo global): abre el flujo existente Elegir castigo → Duración
    (sin motivo; el motivo es "No cumplió la misión del día: <nombre>"). Opción "Sin castigo".
  - **Jugadores** (solo personal): selector de cabezas (multiselección) de jugadores conectados;
    `/staff mision asignar <id> <jugador>` para desconectados.
  - **Guardar** (verde) — global: se añade al final de la cola; personal: se asigna ya y avisa a
    los jugadores. **Cancelar** (rojo).
- Una misión necesita nombre y al menos 1 objetivo para guardarse.
- Editar una misión ya activa no reinicia el progreso de nadie.

### 3.2 Misión global del día
- A **medianoche** (zona horaria del servidor, la misma que reinicia las 6 horas): se evalúa el día
  que termina (§3.5) y se activa la **siguiente de la cola** (la anterior sale de la cola y queda
  en el historial). Si el servidor estaba apagado a medianoche, se hace al arrancar.
- La reciben todos los jugadores no eliminados (los que entren más tarde, al entrar). Cada uno la
  completa por su cuenta y cada uno recibe la recompensa.
- Solo puede completarse una vez por jugador.

### 3.3 Misiones personales
- Asignadas a uno o varios jugadores; sin fecha límite ni castigo. Varias activas a la vez.
- El staff puede retirarlas. Al completarse, desaparecen de ese jugador.

### 3.4 Qué cuenta como "conseguido por él" (crédito personal)
El mod registra, por jugador y por misión activa, cuánto de cada ítem objetivo ha conseguido
**él mismo desde que la misión empezó**:

| Cuenta | Cómo se detecta |
|---|---|
| Minar/picar/cosechar (incl. bloques de mods, fortuna, toque de seda) | ítems soltados por un bloque que rompió él, al recogerlos |
| Fundir/cocinar | sacar él de la salida del horno, ahumador, alto horno |
| Craftear (mesa, inventario y mesas de mods que usan la ranura de resultado) | al recoger el resultado |
| Matar | ítems soltados por un mob que mató él, al recogerlos |
| Pescar | ítems pescados |
| Esquilar, ordeñar, cosechar con la mano (bayas, miel) | ítems resultantes de su interacción |
| Pociones | al sacarlas del soporte de pociones que usó |
| Comerciar con aldeanos | al recoger el resultado del trato |

**No cuenta:** ítems de otros jugadores (soltados o en cofres), botín de cofres de estructuras,
producción de máquinas/tolvas/granjas automáticas, lo que tenía antes de empezar la misión.

**Regla anti‑doble uso:** al completar una misión, a las demás misiones activas del jugador se
les resta del progreso lo entregado de cada ítem (sin bajar de 0).

**Limitación aceptada:** si funde 10 lingotes, los regala y se los devuelven, igual cuentan.

### 3.5 Completar
- Cada 2 s y al recoger un ítem: si en una misión el progreso personal ≥ cantidad **y** tiene esa
  cantidad en el inventario para todos los objetivos → se le quitan exactamente esas cantidades
  y recibe la recompensa (si no cabe, cae a sus pies). Varias misiones listas: primero la global,
  luego las personales por antigüedad.
- Efectos: sonido de logro, mensaje "Misión completada: <nombre>" y aviso al staff.
- Fin del día: quien no completó la global (y no está exento) es **incumplidor**.

### 3.6 Castigo por incumplir la global
- A medianoche, a cada incumplidor se le aplica el **castigo por defecto** de esa misión con el
  sistema de Castigos existente (historial y registro incluidos; el "staff" es "Sistema").
  Si está desconectado: los castigos con duración corren igual; **Expulsar** y **Quitar horas** se
  aplican al conectarse.
- **Exentos:** eliminados (sin vidas) y quien tiene permiso de ausencia para ese día. Quien no se
  conectó y no tiene permiso **sí** es castigado.
- El staff ve la lista en **Incumplidores de ayer** (y un aviso al conectarse): por jugador,
  botones **Perdonar** (quita el castigo automático) y **Castigar más** (flujo de Castigar).

### 3.7 Permisos de ausencia
- Placa → Misiones → Permisos de ausencia → jugador → **Hoy / Mañana / Pasado mañana**.
- `/staff ausencia <jugador> <AAAA-MM-DD>` y `/staff ausencia quitar <jugador> <fecha>`
  (funciona con desconectados).

### 3.8 Lo que ve el jugador
- **Panel (HUD):** apartado Misiones: "Hoy: <nombre> · 32/64" (progreso total de objetivos) y
  "Personales: N". Si no hay global: "Hoy: sin misión".
- **Menú de misiones** (tecla **J**, configurable, o `/misiones`): una fila por misión; cada
  objetivo muestra lo conseguido por él / lo pedido y si lo tiene en el inventario; la recompensa
  visible. Solo lectura.
- **Avisos:** misión nueva (mensaje + sonido), misión completada, y **10 minutos antes de
  medianoche** si no ha cumplido la global ("Te quedan 10 minutos para la misión del día").

### 3.9 Configuración (común)
`avisoMinutosAntes = 10`, `intervaloRevision = 40` ticks.

## 4. Objetivos y anti‑objetivos

**Objetivos:** crear misiones con ítems de cualquier mod sin tocar archivos; que solo cuente lo
conseguido por el propio jugador; que la global se active y se castigue sola aunque no haya
staff conectado; que nada se pierda al reiniciar.

**Anti‑objetivos:** completar con ítems regalados, de cofres o de granjas automáticas; usar el
mismo esfuerzo para dos misiones; castigar a un eliminado o a quien tenía permiso; castigar dos
veces por el mismo día; perder ítems de recompensa; que el staff pierda los ítems al crear.

## 5. No‑objetivos (por ahora)

- Misiones por equipos; objetivos que no sean ítems (matar N mobs sin drop, llegar a un lugar).
- Comparar ítems por NBT en objetivos.
- Detectar el caso de "fundir, regalar y recuperar".
- Tabla de clasificación de misiones.

## 6. Criterios de terminado

1. El staff crea una misión con nombre, objetivos, recompensa, tipo y castigo desde el menú.
2. Solo cuentan ítems conseguidos por el propio jugador según §3.4.
3. Con la misión completa en el inventario se recogen los ítems y se da la recompensa.
4. A medianoche se activa la siguiente global y se castiga a los incumplidores (sin exentos).
5. Perdonar/castigar más desde Incumplidores; permisos de ausencia funcionan.
6. HUD, menú J/`/misiones` y avisos funcionan.
7. `gradlew build test` pasa; lógica pura (crédito, completado, anti‑doble uso, evaluación del
   día, cola) con pruebas.

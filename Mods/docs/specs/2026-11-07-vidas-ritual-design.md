# Diseño: Vidas, aviso dracónico y Altar Dracónico — Senju Core, Forge 1.20.1

Fecha: 2026-11-07
Estado: borrador para revisión

## 1. Intención

El mod es para una **serie de supervivencia extrema**. Cada jugador tiene un número limitado de
vidas. Cada muerte se anuncia a todos con un **aviso dracónico** a pantalla completa. Quien
pierde todas queda **eliminado** (espectador, sin poder volver a entrar) hasta que otros
jugadores lo **resuciten con un ritual** en un Altar Dracónico.

## 2. Decisiones del usuario

- **3 vidas** por jugador (configurable).
- Al morir: aviso en pantalla **para todos** (también para quien murió) con el nombre y las vidas restantes.
- Diseño del aviso: **dracónico, paleta "Dragón de fuego"** (ver §3.2).
- Sin vidas: **modo espectador**; si sale del servidor **no puede volver a entrar** hasta ser revivido.
- Al revivir vuelve con **el número de vidas que se indique**.
- Resurrección por los jugadores: **ritual A, Altar Dracónico** (pilares con fuego, ofrenda, varios
  jugadores, de noche).

## 3. Comportamiento

### 3.1 Vidas
- Cada jugador empieza con `vidasIniciales = 3`. Cada muerte resta 1 (mínimo 0).
- Aplica a **todos** los jugadores, incluido el staff *(mi decisión: en una serie el staff también
  juega; opción `vidasStaff = true` para eximirlos si se pone en `false`)*.
- Persistente (SavedData), sobrevive a reinicios.
- **HUD:** el panel muestra una línea "Vidas" con huevos de dragón (llenos/agrietados) *(mi decisión)*.

### 3.2 Aviso dracónico (cliente)
- Se muestra a todos los jugadores conectados al morir alguien. Duración ~5 s, encima de todo
  menos del menú de pausa; no bloquea el juego.
- Secuencia: oscurecimiento rojizo → **ojo de dragón** (iris ámbar, pupila de reptil) que se abre
  dentro de un **anillo de runas** que gira → **dos alas** que se despliegan → **brasas** que suben
  → **nombre** con letra medieval (fuente Cinzel Decorative, OFL, incluida en el mod) → texto
  "HA CAÍDO · LE QUEDAN N" → **huevos de dragón** (N llenos, el resto agrietados) → se desvanece.
- **Eliminado (0 vidas):** texto "HA SIDO DEVORADO · ELIMINADO", todos los huevos agrietados, pupila cerrada.
- **Resucitado:** misma animación con texto "HA RESUCITADO · VIDAS: N".
- Sonido: rugido del dragón (vanilla, volumen bajo) al aparecer *(mi decisión)*.
- Si llegan varios avisos seguidos, se encolan (uno detrás de otro).
- Opción de cliente `avisoMuerte = true` para desactivarlo.

### 3.3 Eliminación
- Al llegar a 0: pasa a **modo espectador** (sigue conectado, puede mirar).
- Si se desconecta estando eliminado, al intentar volver se le rechaza con
  "Fuiste eliminado de la serie. Espera a que te resuciten."
- No puede salir del modo espectador por su cuenta.

### 3.4 Altar Dracónico (ritual de resurrección)
- **Bloque "Altar Dracónico"**: se fabrica *(receta, mi decisión: obsidiana llorona + bloques de oro +
  ojo de ender + tótem — ver plan)*. Brillo y partículas de fuego suaves.
- **Estructura válida:** el altar con **4 pilares** alrededor (a 2 bloques de distancia en diagonal):
  cada pilar = 2 bloques de obsidiana con **fuego de almas** (o antorcha de almas) encima.
- **Ofrenda:** clic derecho en el altar con un **Pergamino de alma** (papel renombrado en un yunque
  con el nombre exacto del eliminado) mientras se tiene un **Tótem de la inmortalidad** en el
  inventario.
- **Condiciones:** es de **noche** (en el Overworld) y hay **al menos 3 jugadores vivos** (no espectadores)
  a 8 bloques del altar, contando a quien lo activa.
- **Si algo falta:** mensaje claro de lo que falta (estructura, noche, jugadores, ofrenda, nombre no
  eliminado).
- **Éxito:** se consumen el pergamino y el tótem, cae un **rayo decorativo** (sin daño ni fuego),
  explosión de partículas de fuego, y el eliminado **resucita en el altar** con `vidasAlRevivir = 1`
  (configurable). Aviso "HA RESUCITADO" para todos.
- Si el eliminado **no está conectado**: queda marcado como revivido y, al entrar, aparece en el
  altar en supervivencia.

### 3.5 Staff
- `/staff vidas <jugador>` — ver vidas.
- `/staff vidas fijar <jugador> <n>` — fijar vidas (si pasa de 0 a >0, revive).
- `/staff revivir <jugador> <n>` — revive con n vidas (en su punto de reaparición).
- Funciona con desconectados.

### 3.6 Configuración (común)
`vidasIniciales = 3`, `vidasAlRevivir = 1`, `vidasStaff = true`, `jugadoresRitual = 3`,
`ritualDeNoche = true`.

## 4. Objetivos y anti‑objetivos

**Objetivos:** que cada muerte se sienta como un evento de la serie; que la eliminación sea real;
que resucitar sea un logro en equipo.

**Anti‑objetivos:** que un eliminado vuelva a jugar sin ritual o staff (reconectando, cambiando
el modo de juego, con `/gamemode` si no es op); que se pierdan vidas por errores (doble conteo de
una muerte); que el aviso tape la pantalla demasiado o afecte FPS; que el ritual se active por
accidente o con la estructura incompleta.

## 5. No‑objetivos (por ahora)

- Corazón de Dragón (opción B).
- Ganar vidas extra de otras formas.
- Altares múltiples con nombre o teletransporte entre ellos.
- Estadísticas/ranking de muertes.

## 6. Criterios de terminado

1. Cada muerte resta 1 vida y todos ven el aviso dracónico con nombre y vidas.
2. Con 0 vidas: aviso "ELIMINADO", espectador, y no puede reentrar.
3. El ritual completo (estructura + noche + 3 jugadores + tótem + pergamino) resucita con el número de vidas configurado; si falta algo, explica qué.
4. Revivir a un desconectado funciona al conectarse.
5. Comandos de staff para ver/fijar vidas y revivir.
6. El HUD muestra las vidas; el aviso se puede desactivar en el cliente.
7. `gradlew build test` pasa; lógica pura (vidas, estructura del altar, condiciones del ritual) con pruebas.

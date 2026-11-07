# Senju Core — Revisar inventario y Objetos prohibidos: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Desde el Panel de Staff: revisar inventarios (solo lectura, en vivo) y armar en el juego una lista de objetos prohibidos que se confiscan automáticamente a los no staff, con aviso y registro.

**Architecture:** Lógica pura (`ListaProhibidos`, `Paginacion`, `DisposicionInventario`) con JUnit. Lista en `SavedData`. Menús = subclases de `ChestMenu` en el servidor sobre `MenuType.GENERIC_9xN` (sin registrar MenuType), compartiendo una base de solo lectura extraída del `MenuStaff` actual. Confiscación en el servidor (tick + recogida).

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, JUnit 5.

**Spec:** `Minecraft/Mods/docs/specs/2026-11-07-inventario-prohibidos-design.md`

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`; paquetes `com.senju.core.staff.prohibidos`, `com.senju.core.staff.menu`. Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- Clases puras sin imports de `net.minecraft`/`net.minecraftforge`. IDs de ítem como `String` (`"minecraft:diamond"`).
- Todo menú: `stillValid` → `Staff.esStaff`; ningún clic mueve ítems salvo las acciones explícitas de la lista; nunca se suelta nada al suelo; tras cada clic `sendAllDataToRemote()`.
- La Placa de Staff no se puede prohibir.
- Textos (es_es y en_us idénticos):
  - `senjucore.menu.prohibidos` = `Objetos prohibidos` · `senjucore.menu.prohibidos.desc` = `Coloca aquí los ítems que no se pueden tener`
  - `senjucore.menu.selector` = `Elegir jugador` · `senjucore.menu.verinv` = `Inventario de %s` · `senjucore.menu.verender` = `Cofre de ender de %s`
  - `senjucore.menu.volver` = `Volver` · `senjucore.menu.anterior` = `Página anterior` · `senjucore.menu.siguiente` = `Página siguiente`
  - `senjucore.menu.ender` = `Cofre de ender` · `senjucore.menu.actualizar` = `Actualizar`
  - `senjucore.menu.quitar` = `Clic para permitir de nuevo` · `senjucore.menu.marcaprohibido` = `⚠ Objeto prohibido`
  - `senjucore.prohibidos.agregado` = `%s prohibió: %s` · `senjucore.prohibidos.quitado` = `%s permitió: %s`
  - `senjucore.prohibidos.noplaca` = `La Placa de Staff no se puede prohibir.`
  - `senjucore.confiscado.jugador` = `Se te quitó un objeto prohibido: %s x%s` · `senjucore.confiscado.staff` = `Se confiscó a %s: %s x%s`
  - `senjucore.verinv.desconectado` = `%s se desconectó.`
- Registro: `logs/senjucore-confiscados.log` (relativo al directorio del servidor), una línea por confiscación: `yyyy-MM-dd HH:mm:ss | <jugador> (<uuid>) | <id ítem> x<cantidad> | <origen>` con origen ∈ {inventario, armadura, mano_secundaria, cursor, ender}.
- No lanzar `runClient`/`runServer`. Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` solo `Minecraft/`; commits con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Agregar a la lista no consume el ítem del staff** (clic con cursor, shift‑clic, teclas numéricas, arrastre): el ítem vuelve íntegro a su cursor/inventario.
2. **Confiscar nunca crea ItemEntity** y cubre cursor y cofre de ender; staff exento; no confisca la Placa aunque alguien la agregue por NBT.
3. **Jugador revisado se desconecta / cambia de dimensión / muere** con la vista abierta: sin excepción; la vista vuelve al selector.
4. **Lista vacía, una página exacta (45), 46 elementos**: paginación correcta, sin índices fuera de rango.
5. **Vista en vivo** no deja "fantasmas" en el cliente del staff (lo que ve coincide con el inventario real tras cada refresco).

---

### Task 1: Lógica pura y almacenamiento de la lista

**Files:**
- Create: `staff/prohibidos/ListaProhibidos.java` (puro), `staff/menu/Paginacion.java` (puro), `staff/menu/DisposicionInventario.java` (puro), `staff/prohibidos/RegistroProhibidos.java`, `staff/prohibidos/Prohibidos.java`
- Test: `ListaProhibidosTest.java`, `PaginacionTest.java`, `DisposicionInventarioTest.java`

**Interfaces:**
- `ListaProhibidos`: `boolean agregar(String id)` (false si ya estaba), `boolean quitar(String id)`, `boolean contiene(String id)`, `int tamano()`, `List<String> pagina(int pagina, int porPagina)`; orden de inserción.
- `Paginacion`: `static int totalPaginas(int total, int porPagina)` (mín. 1); `static int acotar(int pagina, int total, int porPagina)`.
- `DisposicionInventario`: `static int indiceInventario(int slotMenu)` → índice en `Inventory` o `-1`: 0–26 → 9–35; 27–35 → 0–8; 36→39, 37→38, 38→37, 39→36 (casco a botas); 40 → 40; resto → −1.
- `RegistroProhibidos extends SavedData` (`senjucore_prohibidos`, Overworld) con una `ListaProhibidos`.
- `Prohibidos`: `static boolean esProhibido(MinecraftServer, ItemStack)` (falso para vacío y para la Placa), `static boolean agregar(ServerPlayer staff, ItemStack ejemplo)` (rechaza la Placa con `noplaca`; avisa a todo el staff), `static boolean quitar(ServerPlayer staff, String id)`, `static List<String> pagina(MinecraftServer, int, int)`, `static int tamano(MinecraftServer)`.

- [ ] **Step 1: Tests que fallan:**
  - `ListaProhibidos`: agregar dos veces → true, false; contiene; quitar → true y luego false; `pagina(0,45)` de 46 elementos → 45, `pagina(1,45)` → 1, `pagina(2,45)` → vacío; orden de inserción.
  - `Paginacion`: `totalPaginas(0,45)`=1, `(45,45)`=1, `(46,45)`=2; `acotar(5, 46, 45)`=1; `acotar(-1, 10, 45)`=0.
  - `DisposicionInventario`: 0→9, 26→35, 27→0, 35→8, 36→39, 39→36, 40→40, 41→−1, 53→−1, −999→−1.
- [ ] **Step 2:** FAIL → implementar puros → PASS.
- [ ] **Step 3:** Implementar `RegistroProhibidos` y `Prohibidos` + claves lang de avisos.
- [ ] **Step 4:** `./gradlew build test` → PASS. Commit `feat(senjucore): lista de objetos prohibidos (lógica y almacenamiento)`.

### Task 2: Confiscación automática

**Files:**
- Create: `staff/prohibidos/Confiscacion.java`, `staff/prohibidos/RegistroConfiscados.java` (escritura del log)
- Modify: `SenjuCore.java`, lang

- [ ] **Step 1:** Cada 20 ticks (ServerTick END), para cada jugador no staff: recorrer inventario principal, armadura, mano secundaria, cursor (`containerMenu.getCarried()`) y `getEnderChestInventory()`; cada ítem prohibido se pone a `ItemStack.EMPTY`.
- [ ] **Step 2:** Agrupar lo confiscado por ítem y origen; mensaje al jugador (`confiscado.jugador`), aviso a cada staff conectado (`confiscado.staff`), y una línea por ítem/origen en el log (append, UTF‑8, crear carpeta si falta; errores de E/S se registran con el logger, nunca rompen el tick).
- [ ] **Step 3:** Tras `PlayerEvent.ItemPickupEvent` de un no staff, ejecutar la misma revisión para ese jugador (inmediata).
- [ ] **Step 4:** `./gradlew build test` → PASS. Commit `feat(senjucore): confiscación automática de objetos prohibidos`.

### Task 3: Base de menús de solo lectura, selector y vista de inventario

**Files:**
- Create: `staff/menu/MenuSoloLectura.java` (base abstracta), `staff/menu/MenuSelector.java`, `staff/menu/MenuVerInventario.java`, `staff/menu/MenuVerEnder.java`
- Modify: `staff/menu/MenuStaff.java` (extender la base; botón Revisar inventario → selector), `staff/menu/AccionMenu.java` (+`PROHIBIDOS` en slot 19), `staff/menu/BotonesMenu.java`, lang, README

- [ ] **Step 1:** Extraer de `MenuStaff` una base `MenuSoloLectura extends ChestMenu` (filas N, `SimpleContainer`, `clicked` que nunca llama a `super` y delega en `alClicar(int slot, ClickType tipo, ServerPlayer p)` solo para `PICKUP` en slots del menú, limpia cursor y resincroniza; `quickMoveStack` vacío; `stillValid` staff). `MenuStaff` pasa a usarla sin cambiar comportamiento. Actualizar `AccionMenuTest` para `PROHIBIDOS` (slot 19).
- [ ] **Step 2:** `MenuSelector` (6 filas): cabezas (`Items.PLAYER_HEAD` con perfil del jugador, nombre no cursiva) de los jugadores conectados ordenados por nombre, 45 por página (`Paginacion`); fila 6: Volver (→ Panel) en 45, Anterior 48, Siguiente 50. Clic en cabeza → `MenuVerInventario.abrir(staff, objetivoUUID)`.
- [ ] **Step 3:** `MenuVerInventario` (6 filas): copia del inventario según `DisposicionInventario`; ítems prohibidos con línea de lore roja `marcaprohibido` (en la copia, nunca en el original); 41–44 relleno; 45 Volver (→ selector), 49 Cofre de ender, 53 Actualizar. Se refresca cada 20 ticks y al pulsar Actualizar. Si el objetivo ya no está conectado → mensaje `verinv.desconectado` y volver al selector.
- [ ] **Step 4:** `MenuVerEnder` (3 filas + usa la misma lógica): copia del cofre de ender, Volver (→ vista de inventario) en la casilla 22 tras ampliar a 4 filas *(o 6 filas con fila inferior de botones; elegir la forma más simple y documentarla)*.
- [ ] **Step 5:** `./gradlew build test` → PASS. README (sección + checklist). Commit `feat(senjucore): revisar inventario (selector y vista en vivo)`.

### Task 4: Submenú de la lista de prohibidos

**Files:**
- Create: `staff/menu/MenuProhibidos.java`
- Modify: `MenuStaff.java` (botón Objetos prohibidos → este menú), lang, README

- [ ] **Step 1:** `MenuProhibidos` (6 filas): casillas 0–44 muestran un ítem de cada ID de la página actual con lore gris `quitar`; fila 6: Volver 45, Anterior 48, Siguiente 50.
- [ ] **Step 2:** Agregar: clic `PICKUP` en una casilla del menú con cursor no vacío → `Prohibidos.agregar(staff, cursor)`; **el cursor no cambia** (el ítem del staff se queda en su cursor). Shift‑clic (`QUICK_MOVE`) sobre un slot del **inventario del jugador** → `agregar` con ese ítem, **sin moverlo**. Cualquier otro tipo de clic (SWAP, THROW, QUICK_CRAFT, PICKUP_ALL, CLONE) → ignorado.
- [ ] **Step 3:** Quitar: clic `PICKUP` con cursor vacío sobre un ítem de la lista → `Prohibidos.quitar`. Refrescar la página tras cada cambio.
- [ ] **Step 4:** A diferencia de `MenuSoloLectura`, aquí el cursor NO se limpia (es el ítem real del staff); resincronizar con `sendAllDataToRemote()`. Clics normales sobre el inventario propio del staff (sin shift) deben funcionar como en un cofre normal (mover sus propios ítems), para que pueda tomar el ítem a prohibir: delegar en `super.clicked` **solo** para slots del inventario del jugador y tipos `PICKUP`/`SWAP`/`QUICK_CRAFT`/`PICKUP_ALL` que no involucren slots del menú. Documentar en el report cómo se garantiza que nada entra a las casillas del menú.
- [ ] **Step 5:** `./gradlew build test` → PASS. README (sección + checklist con el diamante). Commit `feat(senjucore): submenú de objetos prohibidos`.

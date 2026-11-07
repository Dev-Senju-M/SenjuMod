# Senju Core — Menú del Staff y Vanish: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** La Placa de Staff abre un menú tipo cofre; su primera función es Vanish (invisible para no staff, sin TAB, sin mensajes de entrada/salida, sin ruido, mobs no atacan, persistente, aviso en el HUD).

**Architecture:** Reglas puras (`ReglaVanish`, `AccionMenu`) con JUnit. Estado en `SavedData`. La ocultación usa **Mixin** donde Forge no da eventos (envío de la entidad, info de TAB, mensajes de entrada/salida). Menú = subclase de `ChestMenu` en el servidor sobre `MenuType.GENERIC_9x3` (el cliente usa el menú vanilla; no hace falta registrar MenuType).

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, Mixin 0.8.5 (incluido en Forge), JUnit 5.

**Spec:** `Minecraft/Mods/docs/specs/2026-11-06-menu-staff-vanish-design.md`

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`. Paquetes nuevos `com.senju.core.staff.vanish`, `com.senju.core.staff.menu`, `com.senju.core.mixin`. Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- Clases puras (`ReglaVanish`, `AccionMenu`) sin imports de `net.minecraft`/`net.minecraftforge`.
- "Staff" siempre vía `Staff.esStaff(...)` (existente).
- Fuentes descompiladas para verificar APIs: `C:\Users\Bryan\AppData\Local\Temp\claude\9630d4d281716964\63db3747-e098-495c-bf45-f4ecbdc1d8df\scratchpad\mcsrc` (parciales; el jar parcheado de Forge está en la caché de Gradle).
- Textos (es_es y en_us idénticos, en español):
  - `senjucore.vanish.on` = `Vanish activado` · `senjucore.vanish.off` = `Vanish desactivado`
  - `senjucore.vanish.aviso.on` = `%s activó Vanish` · `senjucore.vanish.aviso.off` = `%s desactivó Vanish`
  - `senjucore.panel.vanish` = `VANISH`
  - `senjucore.menu.titulo` = `Panel de Staff`
  - `senjucore.menu.castigar` = `Castigar` · `senjucore.menu.inventario` = `Revisar inventario` · `senjucore.menu.cerrar` = `Cerrar`
  - `senjucore.menu.vanish.on` = `Vanish: activado` · `senjucore.menu.vanish.off` = `Vanish: desactivado`
  - `senjucore.menu.vanish.desc` = `Clic para alternar` · `senjucore.menu.proximamente` = `Próximamente`
- No lanzar `runClient` ni `runServer` (los agentes no pueden detenerlos). Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` SOLO rutas bajo `Minecraft/`; commits terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Jugador no staff que entra cuando ya hay alguien en Vanish:** no debe recibir la entidad ni la entrada de TAB del oculto (ni un instante).
2. **Staff que entra estando en Vanish:** nadie no staff ve el mensaje "se unió" ni la entrada de TAB; al salir, tampoco "abandonó".
3. **Alguien deja de ser staff** (deop / quitado de la lista) mientras otro está en Vanish: deja de verlo en ≤ 1 s; y si el que pierde staff estaba en Vanish, se le desactiva.
4. **Desactivar Vanish:** todos vuelven a verte (entidad + TAB) sin reconectar.
5. **Menú:** ningún tipo de clic (shift, arrastre, teclas numéricas 1–9, F de mano secundaria, Q, doble clic, clic central) mueve ítems entre el menú y el inventario.

---

### Task 1: Estado de Vanish, comando, avisos y HUD

**Files:**
- Create: `staff/vanish/ReglaVanish.java`, `staff/vanish/RegistroVanish.java`, `staff/vanish/Vanish.java`
- Create: `network/PaqueteVanish.java`
- Modify: `network/Red.java` (registrar paquete), `cliente/DatosCliente.java` (`boolean vanish()`), `cliente/PanelHud.java` (línea "VANISH"), `staff/ComandoStaff.java` (`/staff vanish`), lang
- Test: `src/test/java/com/senju/core/staff/vanish/ReglaVanishTest.java`

**Interfaces:**
- Produces:
  - `static boolean ReglaVanish.puedeVer(boolean objetivoEnVanish, boolean espectadorEsStaff, boolean mismoJugador)` → `mismoJugador || !objetivoEnVanish || espectadorEsStaff`.
  - `RegistroVanish extends SavedData` (`senjucore_vanish`, Overworld): `static RegistroVanish de(MinecraftServer)`, `boolean contiene(UUID)`, `void poner(UUID, boolean)`.
  - `Vanish`: `static boolean esta(ServerPlayer)`, `static boolean esta(MinecraftServer, UUID)`, `static void alternar(ServerPlayer)`, `static void poner(ServerPlayer, boolean)` (guarda, avisa al jugador y al resto del staff, envía `PaqueteVanish`, y llama al gancho de visibilidad `Vanish.refrescar(ServerPlayer)` — en esta tarea un método vacío documentado que la Tarea 2 rellena).
  - `record PaqueteVanish(boolean activo)` S2C → `DatosCliente.setVanish`.
- [ ] **Step 1: Tests que fallan** (`ReglaVanishTest`): `puedeVer(false,false,false)` → true; `(true,false,false)` → false; `(true,true,false)` → true; `(true,false,true)` → true.
- [ ] **Step 2:** `./gradlew test --tests "*ReglaVanishTest"` → FAIL.
- [ ] **Step 3:** Implementar `ReglaVanish`, `RegistroVanish`, `Vanish`, `PaqueteVanish`, `/staff vanish` (`requires(Staff::esStaff)`), avisos (`on/off` al jugador; `aviso.on/off` a cada otro staff conectado).
- [ ] **Step 4:** Al entrar (`PlayerLoggedInEvent`) enviar `PaqueteVanish(esta(p))`. Cada 20 ticks: si `esta(p) && !Staff.esStaff(p)` → `poner(p,false)`.
- [ ] **Step 5:** HUD: si `DatosCliente.vanish()`, primera línea `VANISH` en morado `0xB58BFF` (parpadeo suave usando `Efectos.parpadeoVisible`; con `rgb=false` igual morado fijo). El ancho del panel la incluye.
- [ ] **Step 6:** `./gradlew build test` → PASS. Commit `feat(senjucore): estado de Vanish, comando y aviso en HUD`.

### Task 2: Ocultación con Mixin (entidad, TAB, mensajes)

**Files:**
- Modify: `build.gradle` (plugin `org.spongepowered.mixin` compatible con ForgeGradle 6, `mixin { add sourceSets.main, 'senjucore.refmap.json'; config 'senjucore.mixins.json' }`, `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`)
- Create: `src/main/resources/senjucore.mixins.json`, mixins en `com.senju.core.mixin`
- Modify: `staff/vanish/Vanish.java` (`refrescar`), eventos de entrada

**Interfaces:**
- Consumes: `Vanish.esta`, `ReglaVanish.puedeVer`, `Staff.esStaff`.

- [ ] **Step 1:** Configurar Mixin (verificar que `./gradlew build` genera el refmap y que el jar declara `MixinConfigs: senjucore.mixins.json` en el manifest).
- [ ] **Step 2: Entidad.** Mixin en `ServerPlayer#broadcastToPlayer(ServerPlayer)` (HEAD, cancelable): si `!ReglaVanish.puedeVer(esta(this), esStaff(viewer), this==viewer)` → `false`. `Vanish.refrescar(p)`: `((ServerLevel)p.level()).getChunkSource().chunkMap.move(p)` para reevaluar quién lo rastrea; además cada 20 ticks refrescar a todos los que estén en Vanish (cubre cambios de staff del espectador).
- [ ] **Step 3: TAB.** Al activar: `ClientboundPlayerInfoRemovePacket(List.of(uuid))` a cada no staff; al desactivar: `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p))` a cada no staff. Mixin en `PlayerList#placeNewPlayer` para que el broadcast de info del jugador que entra, si está en Vanish, solo vaya al staff; y que el nuevo jugador no staff no reciba la info de los que están en Vanish (o recibirla y retirarla en el mismo método antes de enviar el siguiente paquete — sin parpadeo visible).
- [ ] **Step 4: Mensajes.** Mixin para que "X se unió" (en `placeNewPlayer`) y "X abandonó" (en `ServerGamePacketListenerImpl#onDisconnect`) no se envíen a no staff cuando X está en Vanish (el staff sí los ve).
- [ ] **Step 5:** `./gradlew build test` → PASS. README: sección Vanish + checklist con dos jugadores (usar dos clientes no es posible en dev; documentar prueba con un segundo cliente o un servidor real). Commit `feat(senjucore): ocultación de Vanish (entidad, TAB y mensajes)`.

### Task 3: Vanish sin rastro (mobs, ítems, contenedores)

**Files:**
- Create: `staff/vanish/EventosVanish.java`
- Modify: `SenjuCore.java` (registro)

- [ ] **Step 1: Mobs.** `LivingChangeTargetEvent`: si el nuevo objetivo es un jugador en Vanish → cancelar. Cada 20 ticks, a los `Mob` en 32 bloques cuyo `getTarget()` sea un jugador en Vanish → `setTarget(null)`.
- [ ] **Step 2: Ítems.** `EntityItemPickupEvent` con jugador en Vanish → cancelar.
- [ ] **Step 3: Contenedores en silencio.** `PlayerInteractEvent.RightClickBlock` con jugador en Vanish (no agachado con ítem colocable) sobre cofre (incluido doble), cofre trampa, barril o shulker: cancelar y abrir el menú vanilla equivalente (`ChestMenu.threeRows/sixRows`, `ShulkerBoxMenu`) sobre un `Container` proxy que delega en el del bloque pero con `startOpen`/`stopOpen` vacíos (sin sonido ni animación). Respetar candados/`canOpen` del bloque (shulker obstruido, cofre bajo bloque sólido). El cofre de ender queda fuera (no revela nada a otros).
- [ ] **Step 4:** `./gradlew build test` → PASS. README checklist. Commit `feat(senjucore): Vanish sin rastro (mobs, ítems y cofres silenciosos)`.

### Task 4: Menú del staff

**Files:**
- Create: `staff/menu/AccionMenu.java` (puro), `staff/menu/MenuStaff.java`, `staff/menu/BotonesMenu.java`
- Modify: `staff/PlacaStaffItem.java` (`use` abre el menú en vez del mensaje "próximamente"), lang
- Test: `src/test/java/com/senju/core/staff/menu/AccionMenuTest.java`

**Interfaces:**
- Produces: `enum AccionMenu { CASTIGAR, VANISH, INVENTARIO, CERRAR, NINGUNA }` con `static AccionMenu deSlot(int slot)`: 10→CASTIGAR, 13→VANISH, 16→INVENTARIO, 22→CERRAR, resto→NINGUNA. `static void MenuStaff.abrir(ServerPlayer)`.

- [ ] **Step 1: Tests que fallan:** `deSlot(10)`=CASTIGAR, `13`=VANISH, `16`=INVENTARIO, `22`=CERRAR, `0`=NINGUNA, `26`=NINGUNA, `-999`=NINGUNA, `40`=NINGUNA.
- [ ] **Step 2:** FAIL → implementar `AccionMenu` → PASS.
- [ ] **Step 3:** `MenuStaff extends ChestMenu` (servidor, `MenuType.GENERIC_9x3`, 3 filas, `SimpleContainer(27)` relleno con cristal gris sin nombre y los botones de `BotonesMenu`). Sobrescribir `clicked(slotId, button, ClickType, player)`: nunca llamar a `super`; si el slot pertenece al contenedor del menú, ejecutar `AccionMenu.deSlot` (VANISH → `Vanish.alternar` y actualizar el botón; CASTIGAR/INVENTARIO → mensaje `proximamente` en la barra de acción; CERRAR → `closeContainer`); después `sendAllDataToRemote()` para deshacer cualquier predicción del cliente. `quickMoveStack` → `ItemStack.EMPTY`. `stillValid` → `Staff.esStaff(player)`.
- [ ] **Step 4:** Botones: Castigar = `Items.IRON_AXE`, Vanish = poción de invisibilidad (`PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY)`) con brillo si activo y sin texto de efecto (`HideFlags`), Revisar inventario = `Items.CHEST`, Cerrar = `Items.BARRIER`; nombres sin cursiva, descripciones grises.
- [ ] **Step 5:** `PlacaStaffItem.use` → `MenuStaff.abrir(sp)` para staff.
- [ ] **Step 6:** `./gradlew build test` → PASS. README. Commit `feat(senjucore): menú del staff con Vanish`.

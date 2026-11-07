# Senju Core — Placa de Staff: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ítem `senjucore:placa_staff` exclusivo del staff, que no se puede tirar, guardar ni perder, y que desaparece si lo tiene alguien que no es staff.

**Architecture:** Regla pura `ReglaStaff` (JUnit) + puente `Staff` con Minecraft. Ítem registrado con `DeferredRegister`. Toda la protección vive en el servidor (`ProteccionPlaca`, eventos Forge). Comando `/staff placa`.

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, JUnit 5.

**Spec:** `Minecraft/Mods/docs/specs/2026-11-06-placa-staff-design.md`

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`; paquete nuevo `com.senju.core.staff`. Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- `ReglaStaff` no importa nada de `net.minecraft` ni `net.minecraftforge`.
- Config común (`senjucore-common.toml`): `staffIncluyeOperadores = true`, `staff = []` (lista de nombres, comparación sin mayúsculas/minúsculas y sin espacios alrededor).
- Operador = `player.hasPermissions(2)`.
- Textos (es_es y en_us idénticos, en español):
  - `item.senjucore.placa_staff` = `Placa de Staff`
  - `senjucore.placa.desc1` = `Herramienta exclusiva del staff`
  - `senjucore.placa.desc2` = `Clic derecho para abrir el menú`
  - `senjucore.placa.proximamente` = `Menú del staff: próximamente`
  - `senjucore.placa.sinpermiso` = `No tienes permiso para usar la Placa de Staff`
  - `senjucore.placa.dada` = `Placa de Staff entregada a %s.`
  - `senjucore.placa.yatiene` = `%s ya tiene una Placa de Staff.`
  - `senjucore.placa.noesstaff` = `%s no es staff.`
  - `senjucore.placa.lleno` = `El inventario de %s está lleno.`
- No lanzar `runClient` ni `runServer` (los agentes no pueden detenerlos). Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` SOLO de rutas bajo `Minecraft/`; commits terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Tirar arrastrando fuera de la ventana / desde el inventario creativo** (no solo con Q): `onDroppedByPlayer` no cubre todas las vías → además cancelar `ItemTossEvent` y devolver la placa al inventario.
2. **Morir con la placa**: `LivingDropsEvent` debe quitarla de los drops y `PlayerEvent.Clone` (muerte) devolverla; no debe duplicarse si el jugador tenía `keepInventory`.
3. **Inventario lleno al devolverla desde un contenedor**: nunca se suelta al suelo ni se pierde (si no cabe, va al cursor).
4. **Staff al que le quitan el permiso**: pierde la placa en ≤ 1 s, incluida la de la mano secundaria y la del cursor.
5. **Consola** usando `/staff placa <jugador>`: funciona (la consola cuenta como staff).

---

### Task 1: Regla de staff y configuración

**Files:**
- Create: `src/main/java/com/senju/core/staff/ReglaStaff.java`, `src/main/java/com/senju/core/staff/Staff.java`
- Modify: `src/main/java/com/senju/core/config/CommonConfig.java`
- Test: `src/test/java/com/senju/core/staff/ReglaStaffTest.java`

**Interfaces:**
- Produces:
  - `static boolean ReglaStaff.esStaff(boolean esOperador, String nombre, List<? extends String> lista, boolean incluyeOperadores)`
  - `CommonConfig.STAFF_INCLUYE_OPERADORES` (`BooleanValue`, true), `CommonConfig.STAFF` (`ConfigValue<List<? extends String>>`, `[]`, cada elemento `String`).
  - `static boolean Staff.esStaff(ServerPlayer p)`; `static boolean Staff.esStaff(CommandSourceStack s)` — consola/bloque de comandos sin entidad jugador → `s.hasPermission(4)`; con jugador → `esStaff(jugador)`.

- [ ] **Step 1: Tests que fallan** (`ReglaStaffTest`):
  - `esStaff(true, "Ana", List.of(), true)` → true
  - `esStaff(true, "Ana", List.of(), false)` → false
  - `esStaff(false, "Bryant", List.of("bryant"), true)` → true (sin mayúsculas)
  - `esStaff(false, "Bryant", List.of("  Bryant "), false)` → true (recorta espacios)
  - `esStaff(false, "Pepe", List.of("Bryant"), true)` → false
  - `esStaff(false, null, List.of("Bryant"), true)` → false
  - `esStaff(false, "Bryant", null, true)` → false
- [ ] **Step 2:** `./gradlew test --tests "*ReglaStaffTest"` → FAIL.
- [ ] **Step 3:** Implementar `ReglaStaff`, las dos claves de `CommonConfig` y `Staff`.
- [ ] **Step 4:** `./gradlew build test` → PASS.
- [ ] **Step 5:** Commit `feat(senjucore): regla de staff y configuración`.

### Task 2: Ítem Placa de Staff y comando `/staff placa`

**Files:**
- Create: `src/main/java/com/senju/core/staff/PlacaStaffItem.java`, `ItemsMod.java`, `ComandoStaff.java`
- Create: `src/main/resources/assets/senjucore/textures/item/placa_staff.png`, `src/main/resources/assets/senjucore/models/item/placa_staff.json`
- Modify: `SenjuCore.java` (registrar `ItemsMod.ITEMS` en el mod bus; listener de pestaña creativa; comando), lang `es_es.json`/`en_us.json`

**Interfaces:**
- Consumes: `Staff.esStaff(...)` (Task 1).
- Produces:
  - `ItemsMod.ITEMS` (`DeferredRegister<Item>`), `ItemsMod.PLACA_STAFF` (`RegistryObject<Item>`).
  - `static boolean PlacaStaffItem.esPlaca(ItemStack s)`.

- [ ] **Step 1:** Textura 16×16 PNG con transparencia, generada desde este mapa (`.` = transparente) con la paleta `K #1B1B2A, D #3A2A5C, P #6B4BC2, L #A98BFF, W #FFFFFF, G #A8741A, Y #FFD65A` (por ejemplo con PowerShell `System.Drawing`, script en el scratchpad, no en el repo):
  ```
  ................
  ...KKKKKKKKKK...
  ..KGYYYYYYYYGK..
  ..KYDPPPPPPDYK..
  ..KYPPPWWPPPYK..
  ..KYPPWLLWPPYK..
  ..KYPPWLLWPPYK..
  ..KYPPPWWPPPYK..
  ..KYDPPPPPPDYK..
  ...KYDPPPPDYK...
  ....KYDPPDYK....
  .....KYDDYK.....
  ......KYYK......
  .......KK.......
  ................
  ................
  ```
- [ ] **Step 2:** Modelo `{"parent":"minecraft:item/generated","textures":{"layer0":"senjucore:item/placa_staff"}}`.
- [ ] **Step 3:** `PlacaStaffItem extends Item` con `Properties().stacksTo(1).fireResistant()`:
  - `isFoil` → true; `getName` → `Component.translatable(getDescriptionId()).withStyle(ChatFormatting.GOLD)`.
  - `appendHoverText` → `desc1` y `desc2` en `ChatFormatting.GRAY`.
  - `onDroppedByPlayer` → false.
  - `use` → en servidor, si `Staff.esStaff` muestra `proximamente` en la barra de acción; devuelve `InteractionResultHolder.sidedSuccess`/`pass` según corresponda.
- [ ] **Step 4:** Pestaña creativa: en `BuildCreativeModeTabContentsEvent`, si la pestaña es `CreativeModeTabs.OP_BLOCKS` y `event.hasPermissions()` → `accept(PLACA_STAFF)`.
- [ ] **Step 5:** `ComandoStaff`: `/staff placa` (a sí mismo) y `/staff placa <jugador>` (`EntityArgument.player()`), `requires(Staff::esStaff)`. Objetivo no staff → error `noesstaff`; ya tiene placa (en inventario, mano secundaria o cursor) → `yatiene`; si `getInventory().add(...)` falla → `lleno` (nunca soltarla al suelo); si no → `dada`.
- [ ] **Step 6:** Lang con las claves de Global Constraints. `./gradlew build test` → PASS.
- [ ] **Step 7:** Commit `feat(senjucore): ítem Placa de Staff y comando /staff placa`.

### Task 3: Protección de la placa

**Files:**
- Create: `src/main/java/com/senju/core/staff/ProteccionPlaca.java`
- Modify: `SenjuCore.java` (registrar en `MinecraftForge.EVENT_BUS`), `README.md`

**Interfaces:**
- Consumes: `PlacaStaffItem.esPlaca`, `Staff.esStaff`, `ItemsMod.PLACA_STAFF`.

- [ ] **Step 1: Tirar.** `ItemTossEvent`: si la entidad es placa → cancelar y devolver la placa al inventario del jugador (si no cabe, al cursor).
- [ ] **Step 2: Muerte.** `LivingDropsEvent` (jugador): quitar de los drops las placas y guardarlas en un `Map<UUID, List<ItemStack>>`; `PlayerEvent.Clone` con `isWasDeath()`: devolverlas al nuevo jugador y limpiar el mapa. Si `keepInventory` está activo no hay drops → nada que hacer (no duplicar).
- [ ] **Step 3: Contenedores.** `TickEvent.PlayerTickEvent` (servidor, fase END): si `player.containerMenu != player.inventoryMenu`, cualquier slot cuyo `container != player.getInventory()` que tenga placa → vaciarlo y devolver la placa al inventario (si no cabe, al cursor; nunca al suelo).
- [ ] **Step 4: Marcos y soportes.** `PlayerInteractEvent.EntityInteract` y `EntityInteractSpecific`: con placa en la mano usada y objetivo `ItemFrame` o `ArmorStand` → cancelar.
- [ ] **Step 5: No staff.** Cada 20 ticks por jugador: si `!Staff.esStaff(player)`, eliminar todas las placas de inventario (incluye armadura y mano secundaria) y del cursor (`containerMenu.getCarried()`); si se eliminó alguna, mensaje `sinpermiso` en el chat.
- [ ] **Step 6:** README: sección "Placa de Staff" (qué es, `staff`/`staffIncluyeOperadores`, `/staff placa [jugador]`) y checklist: tirar con Q, arrastrar fuera, cofre, tolva vía cofre, shulker, marco, soporte, morir, `/give` a no staff, quitar de `staff`/deop, consola.
- [ ] **Step 7:** `./gradlew build test` → PASS. Commit `feat(senjucore): protección de la Placa de Staff`.

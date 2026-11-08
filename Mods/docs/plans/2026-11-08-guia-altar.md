# Senju Core — Guía de construcción del Altar (bloques fantasma): Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar a quien construye el Altar Dracónico bloques fantasma (transparentes) donde faltan la obsidiana y las antorchas de almas, con progreso y lista de materiales, hasta completar la estructura.

**Architecture:** El servidor calcula el estado de cada posición de la estructura (lógica pura `EstadoGuia` sobre los desplazamientos de `EstructuraAltar`) y lo envía al jugador con `PaqueteGuia` mientras la guía está activa. El cliente guarda la guía y la dibuja en `RenderLevelStageEvent` como bloques translúcidos que laten + contorno (rojo si hay un bloque equivocado).

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, JUnit 5.

**Spec (aprobado por el usuario en chat, 2026-11-08):**
- Se activa al **colocar el altar** (quien lo coloca) y con **clic derecho agachado con la mano vacía** sobre el altar. Solo la ve quien la activó.
- **Bloques fantasma**: obsidiana translúcida en cada hueco de pilar que falte y antorcha de almas translúcida encima de cada pilar; laten suavemente y tienen contorno. Al colocar el bloque correcto su fantasma desaparece. Bloque **equivocado** en una posición → contorno **rojo** en esa posición.
- **Barra de acción**: "Pilares completos: N/4" (actualizado).
- **Chat** al activarse: lista corta de materiales que faltan ("Faltan: 5 obsidianas, 3 antorchas de almas").
- **Termina** al completar la estructura (mensaje "El altar está listo. Realiza el ritual de noche con %s jugadores."), a los **2 minutos**, si el jugador se aleja **más de 32 bloques** o cambia de dimensión, o si el altar se rompe.

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`; paquetes `com.senju.core.vidas.altar` (común/servidor) y `com.senju.core.cliente.guia` (solo cliente). Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- Clases puras sin imports de `net.minecraft`/`net.minecraftforge`.
- Los desplazamientos de la estructura salen de `EstructuraAltar` (única fuente de verdad); no duplicarlos.
- Posición "correcta" de la antorcha: acepta lo mismo que el ritual (`Ritual.tipoLogico` → antorcha de almas, fogata de almas encendida, fuego de almas). El fantasma mostrado es una **antorcha de almas**.
- Textos (es_es y en_us idénticos, español): progreso `Pilares completos: %s/4`, faltan `Faltan: %s obsidiana(s), %s antorcha(s) de almas`, listo `El altar está listo. Realiza el ritual de noche con %s jugadores.`
- Duración 2400 ticks; distancia máx. 32 bloques; refresco del estado cada 10 ticks mientras está activa.
- No lanzar `runClient`/`runServer`. Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` solo `Minecraft/`; commits con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Servidor dedicado: ninguna clase de render en el servidor.
2. Rendimiento: sin trabajo por frame cuando no hay guía; ≤ 12 bloques fantasma dibujados.
3. Estado de render restaurado (blend, depth mask, color) tras dibujar fantasmas.
4. La guía se apaga en todos los casos (completa, tiempo, distancia/dimensión, altar roto, desconexión) y no queda dibujada en el cliente.

---

### Task 1: Guía en el servidor (estado, activación, paquete)

**Files:** Create `vidas/altar/EstadoGuia.java` (puro), `vidas/altar/GuiaAltar.java`, `network/PaqueteGuia.java`; Modify `vidas/altar/EstructuraAltar.java` (exponer la lista de posiciones con su tipo esperado si no existe), `vidas/altar/AltarDraconicoBlock.java` (agachado + mano vacía → activar guía, en lugar de la pista), `network/Red.java`, `SenjuCore.java`, lang. Test `EstadoGuiaTest.java`.

**Interfaces:**
- `enum Esperado { OBSIDIANA, FUEGO_ALMAS }`; `enum EstadoPos { OK, FALTA, INCORRECTO }` (FALTA = aire/reemplazable; INCORRECTO = otro bloque sólido/distinto).
- `EstadoGuia`: `static EstadoPos estado(Esperado e, String tipoLogico)` (tipos lógicos de `EstructuraAltar`: `"obsidiana"`, `"fuego_almas"`, `"aire"`, `"otro"`); `static int pilaresCompletos(...)`; `static int[] faltan(...)` → `{obsidianas, antorchas}` (solo FALTA e INCORRECTO cuentan).
- `record PaqueteGuia(BlockPos altar, List<Entrada> entradas, boolean activa)` con `Entrada(byte dx, byte dy, byte dz, Esperado esperado, EstadoPos estado)`; `activa=false` limpia la guía en el cliente.
- `GuiaAltar` (servidor): `static void activar(ServerPlayer, BlockPos altar)`; mapa `UUID → Guia(dim, altar, venceTick)`; tick cada 10: recalcula, envía `PaqueteGuia` y progreso en la barra de acción; termina (enviando `activa=false`) por completa (+ mensaje listo con `jugadoresRitual`), tiempo, distancia > 32 o otra dimensión, altar ya no es el bloque del altar, desconexión.
- [ ] **Step 1: Tests que fallan:** `estado(OBSIDIANA,"obsidiana")=OK`, `(OBSIDIANA,"aire")=FALTA`, `(OBSIDIANA,"otro")=INCORRECTO`, `(FUEGO_ALMAS,"fuego_almas")=OK`, `(FUEGO_ALMAS,"obsidiana")=INCORRECTO`; `pilaresCompletos` 0/2/4; `faltan` cuenta FALTA+INCORRECTO por tipo.
- [ ] **Step 2:** FAIL → implementar → PASS.
- [ ] **Step 3:** Activación al colocar: `BlockEvent.EntityPlaceEvent` con el bloque del altar y entidad `ServerPlayer` → `activar`. Agachado + mano principal vacía en `AltarDraconicoBlock.use` → `activar` (la pista del pergamino sigue para mano vacía sin agacharse). Al activar: mensaje de materiales que faltan (o "listo" si ya está completa).
- [ ] **Step 4:** `./gradlew build test` → PASS. Commit `feat(senjucore): guía del altar en el servidor`.

### Task 2: Bloques fantasma en el cliente

**Files:** Create `cliente/guia/GuiaCliente.java` (estado recibido), `cliente/guia/RenderGuia.java` (Dist.CLIENT, FORGE bus, `RenderLevelStageEvent`); Modify handler de `PaqueteGuia` (DistExecutor → `GuiaCliente`), `cliente/DatosCliente.limpiar` (o logout) para borrar la guía, README.

- [ ] **Step 1:** `GuiaCliente`: altar + entradas; `limpiar()` al recibir `activa=false`, al desconectarse y al cambiar de dimensión.
- [ ] **Step 2:** `RenderGuia` en la etapa `AFTER_TRANSLUCENT_BLOCKS`: para cada entrada con estado FALTA dibujar el bloque (`Blocks.OBSIDIAN` / `Blocks.SOUL_TORCH` estado por defecto) con `BlockRenderDispatcher.renderSingleBlock` usando un `VertexConsumer` envoltorio que multiplique el alfa (≈ 0.35–0.55 latiendo con `Util.getMillis()`), tipo de render translúcido, luz máxima; trasladar con la cámara (`event.getCamera().getPosition()`). Contorno con `LevelRenderer.renderLineBox` sobre `RenderType.lines()`: blanco/ámbar para FALTA, **rojo** para INCORRECTO (sin bloque fantasma). Restaurar estado (buffers terminados con `endBatch`).
- [ ] **Step 3:** Sin guía → retorno inmediato (sin trabajo).
- [ ] **Step 4:** `./gradlew build test` → PASS. README (sección "Guía de construcción" + checklist). Commit `feat(senjucore): bloques fantasma de la guía del altar`.

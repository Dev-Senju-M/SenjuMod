# Senju Core — Fuente Tektur, máquina de escribir y RGB: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** El panel usa la fuente Tektur, el nombre del servidor/mundo aparece con efecto máquina de escribir, y todo el panel cambia de color en arcoíris (RGB); el tiempo parpadea con < 10 min.

**Architecture:** Lógica de animación pura en `cliente/Efectos.java` (sin Minecraft, con JUnit), calculada a partir de milisegundos de reloj. `PanelHud` la consulta cada frame. La fuente es un recurso del mod (`assets/senjucore/font/panel.json` con proveedor `ttf` + `reference` a `minecraft:default` como respaldo) aplicada solo a los textos del panel mediante `Style.withFont`.

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, JUnit 5.

**Spec (aprobado por el usuario en chat, 2026-11-06):** extensión de `Minecraft/Mods/docs/specs/2026-11-06-panel-jugador-design.md`:
- Fuente **Tektur**, peso normal (sin negrita), solo en el panel; licencia OFL incluida en el mod.
- **Máquina de escribir** en la línea del nombre del servidor/mundo: letra por letra (100 ms/letra) con cursor `_` parpadeante, se queda 3 s completo, se borra (50 ms/letra), pausa 0.5 s y repite.
- **RGB en todo el panel**: el nombre del servidor con un tono por letra (desfase 25°); las demás líneas un tono por línea (desfase 40° por línea); una vuelta completa de color cada 4 s a velocidad 1.0.
- Con RGB activo, la línea del tiempo **parpadea** (500 ms visible / 500 ms oculta) cuando quedan < 10 min (solo en servidor con límite).
- Config cliente nueva: `fuentePersonalizada=true`, `maquinaEscribir=true`, `rgb=true`, `velocidadRgb=1.0` (0.25–4.0). Con `rgb=false` el panel usa los colores anteriores (blanco, tiempo blanco/amarillo/rojo, misiones gris `0xAAAAAA`).
- Sin impacto de FPS: todo se deriva del reloj, sin estado acumulado.

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`; paquete `com.senju.core`. Java 17 toolchain; Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- `Efectos` no importa nada de `net.minecraft`, `net.minecraftforge` ni `java.awt`.
- Colores RGB: HSV con saturación `0.6`, valor `1.0`; componentes con `Math.round(c * 255)`; resultado `0xRRGGBB`.
- No lanzar `runClient` ni `runServer` (no se pueden detener desde los agentes). Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` SOLO de rutas bajo `Minecraft/`. Commits terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Nombre vacío** (servidor aún sin `PaqueteInfoServidor`): `letrasVisibles(ms, 0)` = 0, sin excepción; el panel no se rompe.
2. **Ancho del panel estable** durante la animación: el ancho se calcula con el nombre completo + `_`, no con el texto visible (si no, el panel "respira").
3. **Caracteres que Tektur no tenga**: el proveedor `reference` a `minecraft:default` debe ir después del `ttf` para que haya respaldo (sin cuadros vacíos).
4. **`ms` muy grande** (reloj de muchas horas): sin overflow; usar aritmética `long` y módulo.
5. **RGB apagado**: el comportamiento debe ser exactamente el anterior (colores y sin parpadeo).

---

### Task 1: Lógica pura de efectos

**Files:**
- Create: `src/main/java/com/senju/core/cliente/Efectos.java`
- Test: `src/test/java/com/senju/core/cliente/EfectosTest.java`

**Interfaces:**
- Produces:
  - `static int Efectos.letrasVisibles(long ms, int largo)` — ciclo de duración `largo*100 + 3000 + largo*50 + 500` ms; `t = ms % ciclo`: escribiendo `min(largo, t/100)`; luego `largo` durante 3000 ms; luego borrando `largo - (t - fin)/50` (mín. 0); luego 0. `largo <= 0` → 0.
  - `static boolean Efectos.cursorVisible(long ms)` — `(ms / 400) % 2 == 0`.
  - `static boolean Efectos.parpadeoVisible(long ms)` — `(ms / 500) % 2 == 0`.
  - `static int Efectos.colorRgb(long ms, double velocidad, int desfaseGrados)` — `tono = (ms * velocidad / 4000.0 * 360 + desfaseGrados) mod 360`, HSV(tono, 0.6, 1.0) → `0xRRGGBB`.
  - Constantes públicas `DESFASE_LETRA = 25`, `DESFASE_LINEA = 40`.

- [ ] **Step 1: Tests que fallan** (`EfectosTest`):
  - `letrasVisibles(0, 12)` → 0; `(100, 12)` → 1; `(1200, 12)` → 12; `(4199, 12)` → 12; `(4250, 12)` → 11; `(4800, 12)` → 0; `(5299, 12)` → 0; `(5400, 12)` → 1 (nuevo ciclo, ciclo = 5300).
  - `letrasVisibles(12345, 0)` → 0.
  - `letrasVisibles(Long.MAX_VALUE, 12)` entre 0 y 12 (sin excepción).
  - `cursorVisible(0)` → true; `cursorVisible(400)` → false.
  - `parpadeoVisible(0)` → true; `parpadeoVisible(500)` → false; `parpadeoVisible(1000)` → true.
  - `colorRgb(0, 1.0, 0)` → `0xFF6666`; `colorRgb(0, 1.0, 240)` → `0x6666FF`; `colorRgb(1000, 2.0, 0)` → `0x66FFFF`; `colorRgb(4000, 1.0, 0)` == `colorRgb(0, 1.0, 0)`.
- [ ] **Step 2:** `./gradlew test --tests "*EfectosTest"` → FAIL.
- [ ] **Step 3:** Implementar `Efectos`.
- [ ] **Step 4:** Mismo comando → PASS; `./gradlew test` completo → PASS.
- [ ] **Step 5:** Commit `feat(senjucore): lógica de máquina de escribir y RGB`.

### Task 2: Fuente Tektur, configuración y panel animado

**Files:**
- Create: `src/main/resources/assets/senjucore/font/panel.json`, `src/main/resources/assets/senjucore/font/tektur.ttf`, `src/main/resources/assets/senjucore/font/OFL.txt`
- Modify: `src/main/java/com/senju/core/config/ClientConfig.java`, `src/main/java/com/senju/core/cliente/PanelHud.java`, `README.md`

**Interfaces:**
- Consumes: `Efectos` (Task 1).
- Produces: `ClientConfig.FUENTE_PERSONALIZADA`, `MAQUINA_ESCRIBIR`, `RGB` (`BooleanValue`, default `true`), `VELOCIDAD_RGB` (`DoubleValue`, 1.0, 0.25–4.0).

- [ ] **Step 1:** Descargar Tektur (aprobado por el usuario) del repo oficial `google/fonts`, carpeta `ofl/tektur/` (el archivo es la fuente variable `Tektur[wdth,wght].ttf`; guardarla como `tektur.ttf`) y su `OFL.txt`, en un directorio vacío del scratchpad; copiar a `assets/senjucore/font/`.
- [ ] **Step 2:** `panel.json`: `{"providers":[{"type":"ttf","file":"senjucore:tektur.ttf","shift":[0,1],"size":9.0,"oversample":4.0},{"type":"reference","id":"minecraft:default"}]}` (ajustar `shift`/`size` si es necesario para que la altura visual ≈ fuente vanilla de 9 px; documentar).
- [ ] **Step 3:** Añadir las 4 opciones a `ClientConfig` con los nombres y rangos de arriba.
- [ ] **Step 4:** `PanelHud`:
  - Convertir cada texto a `Component.literal(s)` con `Style.withFont(new ResourceLocation("senjucore","panel"))` si `FUENTE_PERSONALIZADA`; medir anchos con `font.width(Component)` sobre el mismo estilo.
  - `long ms = net.minecraft.Util.getMillis()`.
  - Línea servidor: si `MAQUINA_ESCRIBIR`, mostrar los primeros `letrasVisibles(ms, largo)` caracteres + `_` si `cursorVisible(ms)`; si no, el nombre completo. Ancho del panel calculado con nombre completo + `_` siempre.
  - Si `RGB`: servidor letra por letra con `colorRgb(ms, vel, k * DESFASE_LETRA)`; línea `i` (jugador=1, tiempo=2, hora=3, misiones=4, contando solo las presentes) con `colorRgb(ms, vel, i * DESFASE_LINEA)`; con límite y `restantes < 600`, la línea del tiempo se dibuja solo si `parpadeoVisible(ms)`.
  - Si `!RGB`: colores anteriores exactos (blanco; tiempo `Formato.colorTiempo`; misiones `0xAAAAAA`).
- [ ] **Step 5:** README: sección "Efectos visuales" con las 4 opciones y añadir al checklist: fuente Tektur visible y legible en escala 2 y 3; máquina de escribir; RGB; parpadeo < 10 min (`horasDiarias = 0.15`); `rgb=false` restaura colores; `fuentePersonalizada=false` vuelve a la fuente de Minecraft.
- [ ] **Step 6:** `./gradlew build test` → BUILD SUCCESSFUL.
- [ ] **Step 7:** Commit `feat(senjucore): fuente Tektur y panel con máquina de escribir y RGB`.

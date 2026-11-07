# Senju Core — Vidas, aviso dracónico y Altar Dracónico: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vidas limitadas por jugador, aviso dracónico animado a pantalla completa para todos en cada muerte/eliminación/resurrección, eliminación (espectador + sin reentrada) y resurrección por ritual en un Altar Dracónico o por staff.

**Architecture:** Lógica pura (`ReglaVidas`, `LineaTiempoAviso`, `EstructuraAltar`, `ReglaRitual`) con JUnit. Estado en `SavedData` (`RegistroVidas`). Servicio `Vidas` en el servidor; paquete S2C `PaqueteAviso` a todos; overlay cliente `AvisoDraconico` con cola. Bloque `AltarDraconicoBlock` registrado con `DeferredRegister`.

**Tech Stack:** Forge 1.20.1-47.4.28, mapeos `official`, Java 17, JUnit 5.

**Spec:** `Minecraft/Mods/docs/specs/2026-11-07-vidas-ritual-design.md`

## Global Constraints

- Raíz Gradle `Minecraft/Mods/senjucore/`; paquetes `com.senju.core.vidas` (servidor/común) y `com.senju.core.cliente.aviso` (solo cliente). Gradle con `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot"`.
- Clases puras sin imports de `net.minecraft`/`net.minecraftforge`.
- Config común: `vidasIniciales=3` (1–100), `vidasAlRevivir=1` (1–100), `vidasStaff=true`, `jugadoresRitual=3` (1–20), `ritualDeNoche=true`. Config cliente: `avisoMuerte=true`.
- Paleta "Dragón de fuego": velo `rgba(40,0,0,0.65)`, alas `#2A0A06` con borde `#FF6A1A`, anillo `#FF8A2A`, iris `#FFB11F`/`#FF5A00`, subtítulo `#FFB38A`, brasas `#FF7A1A`/`#FFCF4A`/`#FF3D1A`, huevo `#3B1206` borde `#FF7A1A`, huevo agrietado `#1A1A1A` borde `#444444`.
- Textos (es_es y en_us idénticos): `HA CAÍDO · LE QUEDAN %s`, `HA SIDO DEVORADO · ELIMINADO`, `HA RESUCITADO · VIDAS: %s`, rechazo `Fuiste eliminado de la serie. Espera a que te resuciten.`, mensajes del ritual (falta estructura / no es de noche / faltan jugadores (%s de %s) / falta tótem / pergamino sin nombre / %s no está eliminado / ritual completado).
- Cada muerte se cuenta **una sola vez** (`LivingDeathEvent` de `ServerPlayer`, no cancelado).
- No lanzar `runClient`/`runServer`. Verificar con `./gradlew build test`.
- Git: rama `feat/minecraft-senjucore`; `git add` solo `Minecraft/`; commits con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Tótem de la inmortalidad salva al jugador:** no se descuenta vida (el evento de muerte no ocurre).
2. **Eliminado que reconecta / cambia el modo de juego / muere estando en espectador:** sigue eliminado; sin doble conteo; no puede jugar.
3. **Ritual con estructura incompleta, de día, con menos jugadores, sin tótem o con nombre de alguien no eliminado:** no se consume nada y se explica qué falta.
4. **Varios avisos casi simultáneos** (dos muertes): se encolan y se ven completos, sin parpadeo ni superposición.
5. **Servidor dedicado:** ninguna clase del overlay se carga en el servidor.

---

### Task 1: Vidas (lógica, almacenamiento, muerte, eliminación, comandos, paquete)

**Files:** Create `vidas/ReglaVidas.java` (puro), `vidas/RegistroVidas.java`, `vidas/Vidas.java`, `vidas/EventosVidas.java`, `vidas/ComandoVidas.java` (o subcomandos en `ComandoStaff`), `network/PaqueteAviso.java`; Modify `config/CommonConfig.java`, `network/Red.java`, `SenjuCore.java`, lang. Test `ReglaVidasTest.java`.

**Interfaces:**
- `enum TipoAviso { MUERTE, ELIMINADO, RESUCITADO }`; `record PaqueteAviso(String nombre, int vidas, int total, TipoAviso tipo)` S2C.
- `ReglaVidas`: `static int restar(int vidas)` (mín 0), `static boolean eliminado(int vidas)`, `static TipoAviso avisoTrasMuerte(int vidasDespues)`.
- `RegistroVidas extends SavedData` (`senjucore_vidas`): por UUID: nombre, vidas (inicializa con `vidasIniciales` al primer acceso), `pendienteRevivir` (vidas a dar al entrar) y `posRevivir` (dim+xyz) opcional.
- `Vidas`: `int vidas(server, uuid)`, `void alMorir(ServerPlayer)`, `void revivir(server, GameProfile, int vidas, @Nullable Posicion lugar)`, `void fijar(server, GameProfile, int n)`, `void avisarATodos(server, PaqueteAviso)`, `boolean cuenta(ServerPlayer)` (falso si staff y `vidasStaff=false`).

- [ ] **Step 1: Tests que fallan:** `restar(3)=2`, `restar(1)=0`, `restar(0)=0`; `eliminado(0)` true / `(1)` false; `avisoTrasMuerte(2)=MUERTE`, `(0)=ELIMINADO`.
- [ ] **Step 2:** FAIL → implementar → PASS.
- [ ] **Step 3:** `LivingDeathEvent` (servidor, `ServerPlayer`, no cancelado, `cuenta(p)`, no ya eliminado): restar, guardar, avisar a todos (`MUERTE` o `ELIMINADO`). Si quedó en 0: en `PlayerRespawnEvent` (o al reaparecer) poner `GameType.SPECTATOR` (diferido al tick si hace falta, como en CongelarYCarcel).
- [ ] **Step 4:** Eliminados: en `PlayerLoggedInEvent`, si eliminado y sin `pendienteRevivir` → desconectar con el mensaje de rechazo. Si tiene `pendienteRevivir` → aplicar vidas, `SURVIVAL`, teletransportar a `posRevivir` (si existe), aviso `RESUCITADO`. Cada 20 ticks: un eliminado conectado que no esté en espectador (p. ej. `/gamemode`) vuelve a espectador.
- [ ] **Step 5:** Comandos staff: `/staff vidas <jugador>`, `/staff vidas fijar <jugador> <n>` (0 → elimina; >0 desde 0 → revive), `/staff revivir <jugador> <n>` (online: survival + respawn point; offline: pendiente). `GameProfileArgument`, un solo perfil.
- [ ] **Step 6:** Enviar a cada jugador sus vidas al entrar y al cambiar (reutilizar `PaqueteAviso` no; crear `PaqueteVidas(int vidas, int total)` S2C para el HUD).
- [ ] **Step 7:** `./gradlew build test` → PASS. README. Commit `feat(senjucore): vidas, eliminación y comandos de staff`.

### Task 2: Aviso dracónico (cliente) y vidas en el HUD

**Files:** Create `cliente/aviso/LineaTiempoAviso.java` (puro), `cliente/aviso/AvisoDraconico.java` (overlay + cola + brasas), texturas `assets/senjucore/textures/gui/aviso/{ojo_iris.png, pupila.png, anillo.png, ala.png, huevo.png, huevo_roto.png}` (pixel art generado por script en el scratchpad), fuente `assets/senjucore/font/aviso.json` + `cinzel_decorative.ttf` + `OFL-Cinzel.txt` (Google Fonts, OFL); Modify `cliente/DatosCliente.java`, `cliente/PanelHud.java`, `config/ClientConfig.java` (`AVISO_MUERTE`), handlers de `PaqueteAviso`/`PaqueteVidas`, lang. Test `LineaTiempoAvisoTest.java`.

**Interfaces:**
- `LineaTiempoAviso` (puro, ms desde el inicio, duración total 5600): `float velo(t)` (0→1 en 0–400, se mantiene, 1→0 en 5000–5600), `float emblema(t)` (300–700), `float aperturaOjo(t)` (500–1300, ease‑out cúbico), `float alas(t)` (600–1500, 0.3→1), `float titulo(t)` (1200–1700) y `float espaciadoTitulo(t)` (de +10 a 0 en 1200–2000), `float subtitulo(t)` (1600–2100), `float huevos(t)` (2000–2500), `boolean terminado(t)` (≥ 5600). Todas en [0,1] y multiplicadas por el fundido final.
- [ ] **Step 1: Tests que fallan:** valores en t=0, 400, 1300, 2500, 5300, 5600 para cada curva (0, 1, intermedio con tolerancia), `terminado`.
- [ ] **Step 2:** FAIL → implementar → PASS.
- [ ] **Step 3:** Texturas pixel art (ojo 32×16, pupila 4×16, anillo 64×64 con 12 puntas, ala 48×32, huevo 8×10 y huevo roto) generadas por script fuera del repo con la paleta de Global Constraints.
- [ ] **Step 4:** `AvisoDraconico` (overlay registrado sobre todo excepto si hay pantalla de pausa): cola FIFO de `PaqueteAviso`; dibuja según `LineaTiempoAviso`: velo, alas (escala), anillo (rotación continua), ojo con apertura por recorte (scissor/escala vertical), pupila cerrada si `ELIMINADO`, título con la fuente `senjucore:aviso`, subtítulo según tipo, huevos (N llenos, resto rotos), brasas (≤ 80 partículas simples, posiciones en el cliente). Al empezar cada aviso: sonido `ENDER_DRAGON_GROWL` al 0.4 de volumen. Respeta `AVISO_MUERTE` y F1.
- [ ] **Step 5:** HUD: línea "Vidas" con iconos de huevo (llenos/rotos) a partir de `PaqueteVidas` (oculta si el servidor no envía vidas).
- [ ] **Step 6:** `./gradlew build test` → PASS. README (aviso + checklist). Commit `feat(senjucore): aviso dracónico y vidas en el HUD`.

### Task 3: Altar Dracónico y ritual

**Files:** Create `vidas/altar/EstructuraAltar.java` (puro), `vidas/altar/ReglaRitual.java` (puro), `vidas/altar/AltarDraconicoBlock.java`, `vidas/altar/BloquesMod.java` (DeferredRegister de bloque + BlockItem), modelo/blockstate/textura del altar, receta `data/senjucore/recipes/altar_draconico.json`, `vidas/altar/Ritual.java`; Modify `SenjuCore.java`, lang, README. Tests `EstructuraAltarTest.java`, `ReglaRitualTest.java`.

**Interfaces:**
- `EstructuraAltar`: `static List<String> faltantes(Function<int[], String> bloqueEn)` donde `bloqueEn` recibe un desplazamiento `{dx,dy,dz}` relativo al altar y devuelve un tipo lógico (`"obsidiana"`, `"fuego_almas"`, `"aire"`, `"otro"`); pilares en `(±2, 0, ±2)` y `(±2, 1, ±2)` = obsidiana, `(±2, 2, ±2)` = fuego_almas (fuego o antorcha de almas). Devuelve la lista de pilares incompletos (vacía si válida).
- `ReglaRitual`: `record Condiciones(boolean estructuraOk, boolean esDeNoche, boolean requiereNoche, int jugadoresCerca, int jugadoresNecesarios, boolean tieneTotem, String nombrePergamino, boolean objetivoEliminado)`; `static List<Falta> verificar(Condiciones c)` con `enum Falta { ESTRUCTURA, NOCHE, JUGADORES, TOTEM, PERGAMINO, NO_ELIMINADO }` (vacía = ok).
- [ ] **Step 1: Tests que fallan:** estructura válida → vacía; un pilar sin fuego → 1 faltante; todos los casos de `ReglaRitual` (cada falta sola, varias, todo ok; `requiereNoche=false` ignora la noche).
- [ ] **Step 2:** FAIL → implementar → PASS.
- [ ] **Step 3:** Bloque `senjucore:altar_draconico` (dureza de obsidiana, luz 10, partículas de llama suaves en el cliente), textura pixel art dracónica, receta con forma: fila 1 `O O O` obsidiana llorona, fila 2 `G E G` (oro en bloque, ojo de ender), fila 3 `G T G` (tótem en el centro).
- [ ] **Step 4:** `Ritual`: clic derecho en el altar con papel renombrado: construir `Condiciones` (noche = `level.isNight()` en el Overworld; jugadores vivos no espectadores a ≤ 8 bloques; tótem en el inventario; objetivo por nombre exacto en `RegistroVidas`, eliminado). Si falta algo → mensajes por cada falta, nada se consume. Si ok → consumir 1 papel y 1 tótem, rayo **solo visual** (`LightningBolt.setVisualOnly(true)`), partículas, `Vidas.revivir(server, perfil, vidasAlRevivir, posiciónSobreElAltar)`.
- [ ] **Step 5:** `./gradlew build test` → PASS. README (cómo construir el altar, receta, ritual + checklist). Commit `feat(senjucore): Altar Dracónico y ritual de resurrección`.

# Senju Core — Panel del jugador y límite diario: Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mod Forge 1.20.1 que muestra un HUD con datos del jugador (servidor, cara+nombre, tiempo, hora 12 h, misiones) y aplica un límite de juego diario de 6 h controlado por el servidor.

**Architecture:** Un solo mod `senjucore` instalado en servidor y cliente. La lógica pura (cálculo del tiempo, formato, posiciones) vive en clases sin imports de Minecraft y se prueba con JUnit 5. El servidor lleva el conteo en un `SavedData`, sincroniza al cliente por un `SimpleChannel` propio y expulsa al agotarse; el cliente solo dibuja.

**Tech Stack:** Java 17 (toolchain), Gradle 8.8 (ejecutado con el JDK 21 instalado), ForgeGradle 6, Forge 1.20.1‑47.4.x, mapeos `official`, JUnit Jupiter 5.10.

**Spec:** `Minecraft/Mods/docs/specs/2026-11-06-panel-jugador-design.md`

## Global Constraints

- Raíz del proyecto Gradle: `Minecraft/Mods/senjucore/`. Paquete base `com.senju.core`. Mod id `senjucore`, nombre visible `Senju Core`.
- Minecraft `1.20.1`, Forge `47.4.x` (última 47.x estable), Java toolchain **17**. El equipo tiene JDK 21 y 25 (sin 17): usar el plugin `org.gradle.toolchains.foojay-resolver-convention` 0.8.0 para que Gradle descargue el 17; Gradle se ejecuta con `C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot` (Gradle 8.8 no corre sobre Java 25).
- Clases de lógica pura (`ReglaTiempo`, `EstadoTiempo`, `Formato`, `Posicion`) **no importan nada de `net.minecraft` ni `net.minecraftforge`**.
- Textos al jugador en español, vía `assets/senjucore/lang/es_es.json` y `en_us.json` (mismo contenido en español en ambos por ahora).
- Config común `config/senjucore-common.toml`: `nombreServidor="Mi Servidor"`, `horasDiarias=6.0` (rango 0.01–24), `avisosMinutos=[10, 1]`, `eximirOperadores=false`.
- Config cliente `config/senjucore-client.toml`: `visible=true`, `posicion=ARRIBA_IZQ`, `desplazamientoX=0`, `desplazamientoY=0`, `escala=1.0`.
- Teclas: `H` mostrar/ocultar, `J` siguiente posición; categoría `key.categories.senjucore`.
- Hora en 12 h: `9:45 PM`. Duración: `5h 32m`. Color tiempo: blanco `0xFFFFFF`; amarillo `0xFFFF55` si restante < 30 min; rojo `0xFF5555` si < 10 min.
- Mensaje de expulsión: `Se acabó tu tiempo de hoy. Vuelve después de la medianoche (hora del servidor).`
- El día lo decide **solo el servidor**: `LocalDate.now(ZoneId.systemDefault()).toEpochDay()`. El cliente nunca envía tiempo.
- Git: la rama actual (`feat/elc-carro`) tiene cambios ajenos sin commitear en `Elc/`. Antes de la Tarea 1 crear la rama `feat/minecraft-senjucore` desde HEAD y en cada commit hacer `git add` **solo** de rutas bajo `Minecraft/`. Nunca agregar archivos de `Elc/`.

## Review Focus

1. **Reloj del servidor que retrocede** (ajuste manual/NTP): no debe reiniciar el tiempo del día → `normalizar` solo reinicia si `hoy > estado.dia()`. Test en Tarea 2.
2. **Admin baja `horasDiarias` por debajo de lo ya usado**: restante = 0, sin números negativos, y el jugador es expulsado en el siguiente segundo. Test en Tarea 2.
3. **`/tiempo dar` con más minutos que los usados**: `usados` no baja de 0 (no se acumula tiempo para otros días). Test en Tarea 2.
4. **Pantalla pequeña / GUI scale alto con posición MEDIO o ABAJO**: el panel nunca sale de la pantalla (coordenadas ≥ 0). Test en Tarea 3.
5. **Medianoche con el jugador conectado**: su tiempo vuelve al límite completo sin reconectar, y los avisos vuelven a dispararse ese nuevo día. Test en Tarea 2 (`avisoCruzado` con restantes que suben no avisa).

---

### Task 1: Proyecto Forge base que compila y arranca

**Files:**
- Create: `Minecraft/Mods/senjucore/` a partir del MDK 1.20.1 (`build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- Create: `Minecraft/Mods/senjucore/src/main/java/com/senju/core/SenjuCore.java`
- Create: `Minecraft/Mods/senjucore/src/main/resources/META-INF/mods.toml`, `pack.mcmeta`
- Create: `Minecraft/Mods/senjucore/.gitignore` (`build/`, `run/`, `.gradle/`, `.idea/`, `out/`, `*.iml`)
- Test: `Minecraft/Mods/senjucore/src/test/java/com/senju/core/HumoTest.java`

**Interfaces:**
- Produces: `SenjuCore.MODID = "senjucore"`; constructor `SenjuCore()` que obtiene `FMLJavaModLoadingContext.get().getModEventBus()` (las tareas siguientes registran aquí config, red, overlays y teclas).

- [ ] **Step 1:** Descargar el MDK de Forge 1.20.1‑47.4.x desde `https://files.minecraftforge.net` (pedir permiso al usuario indicando archivo y tamaño). Extraer en una carpeta temporal y copiar a `Minecraft/Mods/senjucore/` solo: wrapper de Gradle, `build.gradle`, `settings.gradle`, `gradle.properties`, `src/main/resources`. Borrar el `ExampleMod` del MDK.
- [ ] **Step 2:** En `gradle.properties` fijar `mod_id=senjucore`, `mod_name=Senju Core`, `mod_group_id=com.senju.core`, `mod_version=0.1.0`, `mod_authors=Bryant`, `mapping_channel=official`. En `gradle/wrapper/gradle-wrapper.properties` usar Gradle `8.8`. En `settings.gradle` añadir el plugin foojay `0.8.0`. En `build.gradle`: `java.toolchain.languageVersion = JavaLanguageVersion.of(17)`, dependencias `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'` y `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`, y `test { useJUnitPlatform() }`.
- [ ] **Step 3:** Test de humo `HumoTest.modidCorrecto()`: `assertEquals("senjucore", SenjuCore.MODID)`.
- [ ] **Step 4:** Crear `SenjuCore` con `@Mod(SenjuCore.MODID)`.
- [ ] **Step 5:** Ejecutar (con `JAVA_HOME` = JDK 21) `./gradlew build test`. Esperado: `BUILD SUCCESSFUL`, 1 test pasado.
- [ ] **Step 6:** Ejecutar `./gradlew genIntellijRuns`. Luego `./gradlew runClient` y verificar que el juego abre y que "Senju Core" aparece en la lista de Mods. Cerrar el juego.
- [ ] **Step 7:** Commit `feat(senjucore): proyecto Forge 1.20.1 base` (solo `Minecraft/`).

### Task 2: Lógica pura del límite diario

**Files:**
- Create: `src/main/java/com/senju/core/tiempo/EstadoTiempo.java`, `ReglaTiempo.java`
- Test: `src/test/java/com/senju/core/tiempo/ReglaTiempoTest.java`

(Rutas relativas a `Minecraft/Mods/senjucore/`.)

**Interfaces:**
- Produces:
  - `record EstadoTiempo(long dia, long usados)` (dia = epochDay, usados en segundos)
  - `static EstadoTiempo ReglaTiempo.normalizar(EstadoTiempo e, long hoy)` — si `hoy > e.dia()` → `new EstadoTiempo(hoy, 0)`; si no, `e` sin cambios.
  - `static EstadoTiempo ReglaTiempo.sumar(EstadoTiempo e, long hoy, long segundos)` — normaliza y suma.
  - `static long ReglaTiempo.restantes(EstadoTiempo e, long limiteSeg)` — `max(0, limite - usados)`.
  - `static boolean ReglaTiempo.agotado(EstadoTiempo e, long limiteSeg)` — `restantes == 0`.
  - `static EstadoTiempo ReglaTiempo.dar(EstadoTiempo e, long minutos)` — `usados = max(0, usados - minutos*60)`.
  - `static OptionalInt ReglaTiempo.avisoCruzado(long restantesAntes, long restantesDespues, List<Integer> avisosMin)` — devuelve el aviso `m` tal que `restantesAntes > m*60 >= restantesDespues`; si varios, el menor.
  - `static long ReglaTiempo.limiteSegundos(double horas)` — `Math.round(horas * 3600)`.

- [ ] **Step 1: Tests que fallan** (`ReglaTiempoTest`), con `LIMITE = 6 * 3600`:
  - `nuevoDiaReinicia`: `normalizar(new EstadoTiempo(100, 5000), 101)` → `(101, 0)`.
  - `mismoDiaConserva`: `normalizar((100, 5000), 100)` → `(100, 5000)`.
  - `relojAtrasNoReinicia`: `normalizar((100, 5000), 99)` → `(100, 5000)`.
  - `sumarAcumula`: `sumar((100, 10), 100, 1)` → usados `11`.
  - `sumarCruzandoMedianoche`: `sumar((100, 21599), 101, 1)` → `(101, 1)`.
  - `restantesNuncaNegativos`: `restantes((100, 30000), LIMITE)` → `0`; `agotado` → `true`.
  - `limiteMenorQueUsado`: `restantes((100, 7200), limiteSegundos(1.0))` → `0`.
  - `darNoBajaDeCero`: `dar((100, 300), 60)` → usados `0`; `dar((100, 7200), 30)` → usados `5400`.
  - `avisoA10`: `avisoCruzado(601, 600, List.of(10, 1))` → `OptionalInt.of(10)`.
  - `avisoA1`: `avisoCruzado(61, 60, List.of(10, 1))` → `OptionalInt.of(1)`.
  - `sinAvisoFueraDeUmbral`: `avisoCruzado(700, 699, List.of(10, 1))` → vacío.
  - `sinAvisoSiSube`: `avisoCruzado(60, 21600, List.of(10, 1))` → vacío.
  - `limiteSeisHoras`: `limiteSegundos(6.0)` → `21600`.
- [ ] **Step 2:** `./gradlew test --tests "*ReglaTiempoTest"` → FAIL (clases inexistentes).
- [ ] **Step 3:** Implementar `EstadoTiempo` y `ReglaTiempo` con las firmas de arriba.
- [ ] **Step 4:** `./gradlew test --tests "*ReglaTiempoTest"` → PASS (13 tests).
- [ ] **Step 5:** Commit `feat(senjucore): reglas del límite diario`.

### Task 3: Lógica pura de formato y posición del panel

**Files:**
- Create: `src/main/java/com/senju/core/cliente/Formato.java`, `Posicion.java`
- Test: `src/test/java/com/senju/core/cliente/FormatoTest.java`, `PosicionTest.java`

**Interfaces:**
- Produces:
  - `static String Formato.duracion(long segundos)` → `"%dh %02dm"` (minutos truncados).
  - `static String Formato.hora12(LocalTime t)` → `"h:mm a"` con `Locale.US` (`AM`/`PM`).
  - `static int Formato.colorTiempo(long restantes)` → `0xFF5555` si `< 600`, `0xFFFF55` si `< 1800`, si no `0xFFFFFF`.
  - `enum Posicion { ARRIBA_IZQ, MEDIO_IZQ, ABAJO_IZQ, ARRIBA_DER, MEDIO_DER, ABAJO_DER }` con `Posicion siguiente()` (cíclico en ese orden) y `int[] origen(int anchoPant, int altoPant, int anchoPanel, int altoPanel, int margen, int despX, int despY)` → `{x, y}`. Izquierda `x = margen + despX`; derecha `x = anchoPant - anchoPanel - margen - despX`; arriba `y = margen + despY`; medio `y = (altoPant - altoPanel) / 2 + despY`; abajo `y = altoPant - altoPanel - margen - despY`. Resultado acotado a `[0, pant - panel]` y nunca < 0.

- [ ] **Step 1: Tests que fallan:**
  - `duracion(19920)` → `"5h 32m"`; `duracion(0)` → `"0h 00m"`; `duracion(21600)` → `"6h 00m"`; `duracion(59)` → `"0h 00m"`.
  - `hora12(LocalTime.of(21,45))` → `"9:45 PM"`; `hora12(LocalTime.of(0,5))` → `"12:05 AM"`; `hora12(LocalTime.of(12,0))` → `"12:00 PM"`.
  - `colorTiempo(599)` → `0xFF5555`; `colorTiempo(1799)` → `0xFFFF55`; `colorTiempo(1800)` → `0xFFFFFF`.
  - `ABAJO_DER.siguiente()` → `ARRIBA_IZQ`; `ABAJO_IZQ.siguiente()` → `ARRIBA_DER`.
  - `MEDIO_IZQ.origen(400, 300, 120, 60, 4, 0, 0)` → `{4, 120}`.
  - `MEDIO_DER.origen(400, 300, 120, 60, 4, 0, 0)` → `{276, 120}`.
  - `ABAJO_DER.origen(400, 300, 120, 60, 4, 0, 0)` → `{276, 236}`.
  - `panelMasGrandeQuePantalla`: `ABAJO_DER.origen(100, 50, 120, 60, 4, 0, 0)` → `{0, 0}`.
- [ ] **Step 2:** `./gradlew test --tests "*FormatoTest" --tests "*PosicionTest"` → FAIL.
- [ ] **Step 3:** Implementar `Formato` y `Posicion`.
- [ ] **Step 4:** Mismo comando → PASS.
- [ ] **Step 5:** Commit `feat(senjucore): formato de hora/tiempo y posiciones del panel`.

### Task 4: Configuración y red

**Files:**
- Create: `src/main/java/com/senju/core/config/CommonConfig.java`, `ClientConfig.java`
- Create: `src/main/java/com/senju/core/network/Red.java`, `PaqueteInfoServidor.java`, `PaqueteTiempo.java`
- Create: `src/main/java/com/senju/core/cliente/DatosCliente.java`
- Modify: `SenjuCore.java` (registrar configs `ModConfig.Type.COMMON` / `CLIENT` y llamar `Red.registrar()` en `FMLCommonSetupEvent`)

**Interfaces:**
- Consumes: `Posicion` (Tarea 3).
- Produces:
  - `CommonConfig.NOMBRE_SERVIDOR` (`ConfigValue<String>`), `HORAS_DIARIAS` (`DoubleValue`), `AVISOS_MINUTOS` (`ConfigValue<List<? extends Integer>>`), `EXIMIR_OPERADORES` (`BooleanValue`), `CommonConfig.SPEC`.
  - `ClientConfig.VISIBLE`, `POSICION` (`EnumValue<Posicion>`), `DESPLAZAMIENTO_X`, `DESPLAZAMIENTO_Y` (`IntValue`), `ESCALA` (`DoubleValue` 0.5–2.0), `ClientConfig.SPEC`.
  - `Red.CANAL` (`SimpleChannel`, nombre `senjucore:main`, `PROTOCOLO = "1"`, cliente y servidor aceptan **solo** `PROTOCOLO::equals` → el mod es obligatorio en ambos lados); `static void Red.registrar()`; `static void Red.enviarA(ServerPlayer p, Object paquete)`.
  - `record PaqueteInfoServidor(String nombreServidor, long limiteSegundos, boolean sinLimite)` con `encode/decode/handle`.
  - `record PaqueteTiempo(long restantes, long usadosHoy)` con `encode/decode/handle`.
  - `DatosCliente` (solo cliente, estático): `nombreServidor()`, `sinLimite()`, `restantes()`, `usadosHoy()`, `void tick()` (cada 20 ticks de cliente: `restantes--` mín. 0, `usadosHoy++`), `boolean tieneDatos()`, `void limpiar()` (al desconectar). Los `handle` de los paquetes escriben aquí usando `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`.

- [ ] **Step 1:** Implementar las configs con los valores por defecto de Global Constraints y registrarlas en `SenjuCore`.
- [ ] **Step 2:** Implementar `Red`, los dos paquetes y `DatosCliente`.
- [ ] **Step 3:** `./gradlew build` → `BUILD SUCCESSFUL`. `./gradlew runServer` una vez (aceptar EULA en `run/eula.txt` solo con permiso del usuario) → se crea `run/config/senjucore-common.toml` con las 4 claves. Detener el servidor.
- [ ] **Step 4:** Commit `feat(senjucore): configuración y canal de red`.

### Task 5: Control del tiempo en el servidor

**Files:**
- Create: `src/main/java/com/senju/core/tiempo/RegistroTiempo.java`, `ControlTiempo.java`
- Modify: `SenjuCore.java` (registrar `ControlTiempo` en `MinecraftForge.EVENT_BUS`)
- Modify: `assets/senjucore/lang/es_es.json`, `en_us.json`

**Interfaces:**
- Consumes: `ReglaTiempo`/`EstadoTiempo` (T2), `CommonConfig`, `Red`, paquetes (T4).
- Produces:
  - `RegistroTiempo extends SavedData`, nombre de datos `senjucore_tiempo`: `static RegistroTiempo de(MinecraftServer s)` (Overworld `computeIfAbsent`), `EstadoTiempo get(UUID)`, `void set(UUID, EstadoTiempo)` (marca `setDirty()`). NBT: lista de `{uuid, dia, usados}`.
  - `ControlTiempo`: `static long hoy()`; `static boolean aplicaLimite(ServerPlayer p)` → `false` si `!server.isDedicatedServer()` o (`EXIMIR_OPERADORES` y `server.getPlayerList().isOp(p.getGameProfile())`); `static void sincronizar(ServerPlayer p)` (envía `PaqueteTiempo`); `static void expulsar(ServerPlayer p)` → `p.connection.disconnect(Component.translatable("senjucore.expulsado"))`.
  - Claves lang: `senjucore.expulsado` (texto exacto de Global Constraints), `senjucore.aviso` = `Te quedan %s minutos de juego hoy.`

- [ ] **Step 1:** Implementar `RegistroTiempo`.
- [ ] **Step 2:** Implementar `ControlTiempo`:
  - `PlayerLoggedInEvent`: normalizar estado; enviar `PaqueteInfoServidor(nombre, limite, !aplicaLimite)`; si `aplicaLimite && agotado` → `expulsar`; si no → `sincronizar`.
  - `ServerTickEvent` (fase END, cada 20 ticks): por cada jugador conectado, `sumar(estado, hoy(), 1)`; si aplica límite: comprobar `avisoCruzado` → mensaje de chat `senjucore.aviso` + `sincronizar`; si `agotado` → `expulsar`. Cada 30 s → `sincronizar` a todos. Si `normalizar` cambió de día → `sincronizar` inmediato.
- [ ] **Step 3:** `./gradlew build` → OK; `./gradlew test` → todo PASS.
- [ ] **Step 4: Prueba manual** con `horasDiarias = 0.05` (3 min) en `run/config/senjucore-common.toml`: `runServer` + `runClient`, conectar a `localhost`. Esperado: aviso de 1 min en el chat aprox. a los 2 min, expulsión a los 3 min con el mensaje exacto, y rechazo al reconectar. Reiniciar el servidor y reconectar → sigue rechazado (persistencia).
- [ ] **Step 5:** Commit `feat(senjucore): límite diario en el servidor`.

### Task 6: Comandos `/tiempo`

**Files:**
- Create: `src/main/java/com/senju/core/tiempo/ComandoTiempo.java`
- Modify: `SenjuCore.java` (escuchar `RegisterCommandsEvent`), archivos lang

**Interfaces:**
- Consumes: `RegistroTiempo`, `ReglaTiempo`, `ControlTiempo.sincronizar`.
- Produces: `/tiempo ver <jugador>`, `/tiempo reiniciar <jugador>`, `/tiempo dar <jugador> <minutos>` (`IntegerArgumentType.integer(1, 1440)`), todos con `requires(s -> s.hasPermission(2))`, argumento `EntityArgument.player()`. Respuestas: `ver` → `<jugador>: usado 1h 05m, restante 4h 55m` (usar `Formato.duracion`); `reiniciar`/`dar` → confirmación y `sincronizar` al jugador objetivo.

- [ ] **Step 1:** Implementar el comando y sus claves lang.
- [ ] **Step 2:** `./gradlew build` → OK.
- [ ] **Step 3: Prueba manual:** en `runServer`, consola: `op Dev`; desde el cliente `/tiempo ver Dev`, `/tiempo dar Dev 5` → el restante del panel/comando sube 5 min; `/tiempo reiniciar Dev` → restante = límite completo. Un jugador no‑op recibe "Unknown command" / sin permiso.
- [ ] **Step 4:** Commit `feat(senjucore): comandos /tiempo para administradores`.

### Task 7: Panel HUD y teclas

**Files:**
- Create: `src/main/java/com/senju/core/cliente/PanelHud.java`, `Teclas.java`, `EventosCliente.java`
- Modify: `SenjuCore.java` (solo en `Dist.CLIENT`: `RegisterGuiOverlaysEvent`, `RegisterKeyMappingsEvent`), archivos lang

**Interfaces:**
- Consumes: `DatosCliente` (T4), `Formato`, `Posicion` (T3), `ClientConfig` (T4).
- Produces:
  - `Teclas.MOSTRAR` (`GLFW_KEY_H`), `Teclas.POSICION` (`GLFW_KEY_J`), categoría `key.categories.senjucore`.
  - `EventosCliente`: `ClientTickEvent` → `DatosCliente.tick()`, consumir clics de teclas (alternar `VISIBLE`; `POSICION.set(POSICION.get().siguiente())`; guardar con `ClientConfig.SPEC.save()`); `ClientPlayerNetworkEvent.LoggingOut` → `DatosCliente.limpiar()`.
  - `PanelHud` (`IGuiOverlay`, id `senjucore:panel`, registrado con `registerAboveAll`).

- [ ] **Step 1:** Implementar `Teclas` y `EventosCliente`.
- [ ] **Step 2:** Implementar `PanelHud.render`: salir si `!VISIBLE`, `mc.options.hideGui`, o `mc.screen != null` (excepto `ChatScreen`). Líneas en orden: nombre servidor; cara 8×8 con `PlayerFaceRenderer.draw(gfx, skin, x, y, 8)` + nombre del jugador; tiempo; `Hora: ` + `Formato.hora12(LocalTime.now())`; `Misiones: Próximamente` (gris `0xAAAAAA`). Fondo `0x80000000`, margen interno 4 px, ancho = línea más larga + 8. Origen con `Posicion.origen(...)` (margen 4), escala con `pose().scale`.
  - Servidor con límite: `Tiempo restante: ` + `duracion(restantes)` en `colorTiempo(restantes)`.
  - `sinLimite` (individual): `Tiempo jugado hoy: ` + `duracion(usadosHoy)` en blanco; nombre del servidor = `mc.getSingleplayerServer().getWorldData().getLevelName()`.
  - Si `!DatosCliente.tieneDatos()`: no dibujar línea de tiempo.
- [ ] **Step 3:** `./gradlew build` → OK.
- [ ] **Step 4: Prueba manual (multijugador):** `runServer` + `runClient` con `nombreServidor = "Senju Test"`. Esperado: panel con "Senju Test", cara + nombre, tiempo bajando cada segundo, hora 12 h correcta, "Misiones: Próximamente". `J` recorre las 6 posiciones (incluidas medio‑izq y medio‑der) sin salir de pantalla con GUI scale 4; `H` oculta/muestra; tras reiniciar el cliente se conserva posición/visibilidad. El color pasa a amarillo/rojo al bajar (usar `horasDiarias = 0.5`).
- [ ] **Step 5: Prueba manual (individual):** crear mundo nuevo → panel muestra el nombre del mundo y `Tiempo jugado hoy` subiendo; nunca expulsa.
- [ ] **Step 6:** Commit `feat(senjucore): panel HUD con posiciones y teclas`.

### Task 8: Verificación final y guía de uso

**Files:**
- Create: `Minecraft/Mods/senjucore/README.md` (cómo abrir en IntelliJ con JDK 21 para Gradle, `genIntellijRuns`, `runClient`/`runServer`, dónde está cada config, comandos, cómo compilar el `.jar` con `gradlew build` → `build/libs/`).

- [ ] **Step 1:** `./gradlew clean build test` → `BUILD SUCCESSFUL`, todos los tests pasan.
- [ ] **Step 2:** Recorrer los 7 criterios de "terminado" del spec (§7) y anotar el resultado de cada uno.
- [ ] **Step 3:** Escribir `README.md`.
- [ ] **Step 4:** Commit `docs(senjucore): guía de uso y desarrollo`.

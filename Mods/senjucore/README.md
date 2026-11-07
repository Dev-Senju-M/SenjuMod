# Senju Core

Mod de Minecraft Forge 1.20.1 (Forge 47.4.28). Muestra un panel (HUD) con el nombre del servidor,
tu cara y nombre, el tiempo restante (o jugado hoy), la hora y un marcador de misiones, y aplica un
límite diario de juego en servidores.

> El mod debe estar instalado en el servidor Y en cada cliente.

## Abrir en IntelliJ IDEA

1. `File > Open` y elige la carpeta `Minecraft/Mods/senjucore` (el `build.gradle`).
2. Antes de sincronizar: `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM`
   = `C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot` (JDK 21).
   El Java 25 por defecto NO puede ejecutar Gradle 8.8. Gradle descarga solo el Java 17 que usa el juego (toolchain).
3. Sincroniza Gradle y ejecuta en terminal: `gradlew genIntellijRuns`. Esto crea las configuraciones
   de ejecución `runClient` y `runServer`.
4. Por línea de comandos (Git Bash) también puedes usar:
   `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot" ./gradlew runClient`

## Compilar el .jar

```
gradlew clean build test
```

El mod queda en `build/libs/senjucore-0.1.0.jar`. Cópialo a la carpeta `mods/` del servidor y de cada cliente.

## Configuración

Se generan en `run/config/` (desarrollo) o `config/` (instalación normal) al arrancar.

`senjucore-common.toml` (servidor):

| Clave | Por defecto | Descripción |
|---|---|---|
| `nombreServidor` | `"Mi Servidor"` | Nombre mostrado en el panel |
| `horasDiarias` | `6.0` (0.01 a 24) | Horas de juego permitidas por día |
| `avisosMinutos` | `[10, 1]` | Minutos restantes en que se avisa en el chat |
| `eximirOperadores` | `false` | Si es `true`, los operadores no tienen límite |

`senjucore-client.toml` (cada jugador):

| Clave | Por defecto | Descripción |
|---|---|---|
| `visible` | `true` | Panel visible |
| `posicion` | `ARRIBA_IZQ` | `ARRIBA_IZQ, MEDIO_IZQ, ABAJO_IZQ, ARRIBA_DER, MEDIO_DER, ABAJO_DER` |
| `desplazamientoX` / `desplazamientoY` | `0` | Ajuste fino en píxeles |
| `escala` | `1.0` (0.5 a 2.0) | Tamaño del panel |
| `fuentePersonalizada` | `true` | Usa la fuente Tektur en el panel (`false` = fuente de Minecraft) |
| `maquinaEscribir` | `true` | El nombre del servidor se escribe y borra como en una máquina de escribir |
| `rgb` | `true` | Colores RGB animados (`false` = colores clásicos, sin parpadeo) |
| `velocidadRgb` | `1.0` (0.25 a 4.0) | Velocidad del ciclo de colores |

## Efectos visuales

Las cuatro opciones de efectos están en `senjucore-client.toml` (tabla anterior). La fuente es Tektur (SIL OFL 1.1, ver `src/main/resources/assets/senjucore/font/OFL.txt`) y solo se aplica al panel. Con `rgb = true`, cada letra del nombre del servidor y cada línea tienen su propio tono; con límite diario y menos de 10 min restantes la línea de tiempo parpadea. Si la fuente se ve más grande o pequeña que la de Minecraft, ajusta `size`, `shift` u `oversample` en `src/main/resources/assets/senjucore/font/panel.json` (valores actuales: `size` 13.0, `shift` [0, 0], `oversample` 4.0; si los trazos finos se ven rotos con escala de GUI 1–2, baja `oversample` a 2–3; no se verificó el tamaño visual en el juego).

## Teclas (configurables en Controles > "Senju Core")

- `H`: mostrar u ocultar el panel.
- `J`: cambiar a la siguiente posición (las 6 en ciclo). Se recuerda al reiniciar el juego.

## Comandos (solo operadores, nivel 2)

- `/tiempo ver <jugador>`: tiempo usado y restante hoy.
- `/tiempo reiniciar <jugador>`: pone el tiempo usado a 0.
- `/tiempo dar <jugador> <minutos>`: da de 1 a 1440 minutos extra (hasta el máximo diario).

Funcionan también con jugadores desconectados (por ejemplo, uno ya expulsado por agotar su tiempo).

### Comandos de staff (`/staff ...`, solo staff)

- `/staff perdonar <jugador> [silencio|ban|congelar|carcel]`: quita los castigos activos (todos o solo el indicado). Responde "Perdonado: X" o "X no tenía castigos activos". Funciona con desconectados (por perfil). Expulsar y Quitar horas no tienen estado que perdonar.
- `/staff historial <jugador>`: últimos 10 registros, del más reciente: `yyyy-MM-dd HH:mm · <acción> <tipo> (<duración>) · <motivo> · por <staff>`; sin registros: "Sin historial".
- `/staff carcel fijar`: fija la cárcel en tu posición (dimensión, xyz y rotación). Solo jugadores (la consola recibe error).
- `/staff banear <jugador> <duración> <motivo...>`: banea también a desconectados y desde la consola. Duración `10m`, `1h`, `1d`, `7d` o `perm` (`<n>m|h|d`); si es inválida muestra los ejemplos. El motivo es texto libre. No se puede banear a staff.

## Comportamiento

- Servidor: el tiempo restante baja cada segundo, avisa en `avisosMinutos` y expulsa al llegar a 0.
  Reentrar el mismo día te expulsa con mensaje; a medianoche (hora del servidor) vuelve el tiempo completo.
  El tiempo usado se guarda en el mundo y sobrevive a reinicios.
- Un jugador: no hay límite; el panel muestra el nombre del mundo y el tiempo jugado hoy.
- El panel se oculta con F1 y al abrir inventarios o menús (el chat lo mantiene visible).

## Placa de Staff

Ítem `senjucore:placa_staff` exclusivo del staff (clic derecho abre el menú del staff, ver abajo).
Es staff quien sea operador nivel 2+ (si `staffIncluyeOperadores = true`) o esté en la lista `staff`
(nombres, sin distinguir mayúsculas) de `senjucore-common.toml`. Se obtiene con `/staff placa [jugador]`
o desde la pestaña creativa de operadores.

Protección (servidor): no se tira (Q, arrastrar fuera, creativo), no se queda en contenedores, marcos ni
soportes ni en un Allay (clic derecho bloqueado), se conserva al morir, y a quien no es staff se le elimina (revisión cada 1 s) con aviso.
Si no cabe en el inventario queda pendiente en memoria y el siguiente tick del jugador la pone en un
hueco libre o, si no lo hay, en el cursor cuando está vacío; si no, espera (nunca se suelta al suelo). Las placas apartadas al morir viven en memoria: si el
servidor se reinicia con un jugador muerto sin reaparecer, se pierden, igual que las guardadas para un jugador desconectado (`pendientes`); se recupera con `/staff placa`.

Limitación: en modo creativo, la papelera o la cuadrícula de ítems del inventario creativo pueden
borrar la placa, y el clic central puede clonarla (el creativo lo controla el cliente, el servidor no
se entera); se recupera con `/staff placa`. Las pruebas de protección (morir, tirar, cofres) hazlas en
**supervivencia**. Los contenedores se revisan al inicio
de cada tick del servidor (antes que las tolvas) y, si un no staff ve la placa en un contenedor, se descarta.

## Menú del staff

Clic derecho con la Placa abre "Panel de Staff" (cofre de 3 filas; `MenuStaff` sobre `MenuType.GENERIC_9x3`, el cliente usa la pantalla vanilla). Botones: Castigar (slot 10) abre el asistente de castigo (ver abajo); Objetos prohibidos (19, libro y pluma) abre la lista de prohibidos; Revisar inventario (16) abre el selector de jugador; Vanish (13, poción: brilla si está activo) alterna Vanish; Cerrar (22). `clicked` nunca llama a `super`: ningún tipo de clic mueve ítems ni deja nada en el cursor; solo un clic simple (PICKUP) en un botón actúa. Se cierra solo si pierdes staff. Los botones nunca son placas (compatible con `ProteccionPlaca`).

### Revisar inventario

Los menús de solo lectura del staff (Panel, selector y vistas; no la lista de prohibidos) extienden `MenuSoloLectura` (base común: `clicked` sin `super`, solo PICKUP en slots del menú llega a `alClicar`, `quickMoveStack` vacío, `stillValid` = staff, cursor limpio y `sendAllDataToRemote()` tras cada clic). Revisar inventario (Panel, slot 16) abre `MenuSelector`: cabezas (con el perfil y skin) de los jugadores conectados por orden alfabético, 45 por página; Volver (45), Página anterior (48), Página siguiente (50). Clic en una cabeza abre `MenuVerInventario`: copia del inventario con la misma disposición que `DisposicionInventario` (0-26 principal, 27-35 barra, 36-39 casco a botas, 40 mano secundaria), relleno en 41-44; Volver (45) regresa al selector en la misma página, Cofre de ender (49) abre `MenuVerEnder` (6 filas: contenido en las 3 primeras, Volver en 45 hacia la vista de inventario), Actualizar (53). Los ítems prohibidos llevan una línea roja "Objeto prohibido" solo en la copia. La vista se refresca cada 20 ticks desde `broadcastChanges()` del propio menú (se reconstruyen las copias y solo viajan los slots que cambian); el objetivo se resuelve por UUID en cada refresco, y si se desconectó se avisa y se vuelve al selector (muerte y cambio de dimensión no dejan referencias obsoletas).

Placa de Staff: nunca se coloca una copia de la Placa en un contenedor de menú, porque `ProteccionPlaca` revisa los slots ajenos cada tick y se la "extraería" al espectador, duplicándola. En su lugar se muestra un papel gris "Placa de Staff" (`CopiaVista`).

Modo espectador: un staff en espectador puede abrir estos menús pero no hacer clic en ellos (vanilla ignora los clics en contenedores de los jugadores en espectador y solo resincroniza).

### Objetos prohibidos

Panel > Objetos prohibidos (slot 19) abre `MenuProhibidos` ("Objetos prohibidos", 6 filas sobre `MenuType.GENERIC_9x6`): las casillas 0-44 muestran un ítem nuevo de cada tipo prohibido (orden de inserción, 45 por página) con la línea gris "Clic para permitir de nuevo"; Volver (45) regresa al Panel, Página anterior (48) y Página siguiente (50). La página se reconstruye tras cada clic y cada 20 ticks (otro staff puede cambiar la lista). Un ID que ya no existe en el registro (mod quitado) o que no se puede mostrar se ve como barrera con el ID; clic la quita de la lista.

- **Agregar:** toma el ítem de tu inventario y haz clic (izquierdo o derecho) en cualquier casilla 0-44, o shift+clic sobre el ítem en tu inventario. Se agrega el **tipo**; el ítem no se mueve ni se gasta (se queda en el cursor o en su slot). La Placa de Staff no se puede agregar.
- **Quitar:** clic con el cursor vacío sobre un ítem de la lista.
- Si el tipo ya estaba en la lista, la barra de acción muestra "Diamante ya está prohibido."
- Agregar/quitar avisa a todo el staff conectado ("S prohibió: Diamante" / "S permitió: Diamante").

Confiscación (`Confiscacion`, solo no staff): cada 20 ticks, al recoger un ítem y al desconectarse se borran los objetos prohibidos del inventario, armadura, mano secundaria, cursor, cofre de ender y rejillas de crafteo (la 2x2 y la entrada de mesa de crafteo, yunque, herrería, afilador, cortapiedras, telar, cartografía y encantamiento; los slots de resultado se ignoran). Tirar un objeto prohibido (Q, arrastrar fuera) cancela el evento, y al morir se retiran de los drops: nunca llega al suelo. Cada confiscación se anota en `logs/senjucore-confiscados.log` con el origen: `inventario`, `armadura`, `mano_secundaria`, `cursor`, `ender`, `crafteo`, `tirado` o `muerte`. Si el objeto prohibido es un contenedor (p. ej. una shulker box), se borra con todo su contenido y solo se registra el contenedor.

A diferencia de `MenuSoloLectura`, este menú extiende `AbstractContainerMenu` directamente (mismo orden de slots que `ChestMenu`) porque el cursor es el ítem real del staff y no se limpia. Nada entra ni sale de las casillas del menú: son `SlotFijo` (`mayPlace` y `mayPickup` = false), `canTakeItemForPickAll`/`canDragTo` = false para ellas, `quickMoveStack` no mueve nada, y `clicked` solo llama a `super` para slots del inventario del jugador con PICKUP, SWAP (teclas 1-9/F) y PICKUP_ALL (doble clic). Soltar con Q/Ctrl+Q (THROW; nunca se suelta nada al suelo), arrastre (QUICK_CRAFT), clic central (CLONE), clics fuera de la ventana y cualquier otro tipo sobre las casillas del menú se ignoran. Tras cada clic, `sendAllDataToRemote()`.

Límite (vanilla): si cierras el menú con un ítem en el cursor y el inventario lleno (o al morir/desconectarte con él en el cursor), `removed()` de vanilla lo suelta al suelo, igual que con cualquier cofre.

## Vanish

`/staff vanish` (solo staff) alterna Vanish; el estado se guarda en el mundo y sobrevive a reconexiones y reinicios.
En Vanish, los jugadores **no staff**: no reciben tu entidad (no te ven ni a tu armadura), no te ven en TAB y no
ven "se unió"/"abandonó la partida" cuando entras o sales. El staff te ve normal y sí recibe esos mensajes.
Al desactivarlo todos vuelven a verte sin reconectar. Si alguien gana o pierde staff, en <= 1 s ve (o deja de ver)
a los que están en Vanish; si el que pierde staff estaba en Vanish, se le desactiva.

Se hace con Mixin (`com.senju.core.mixin`, config `senjucore.mixins.json`): `ServerPlayer#broadcastToPlayer`
(entidad) y `#die` (mensaje de muerte), `PlayerList#placeNewPlayer` (TAB y mensaje de entrada) y `#broadcast`
(sonidos), `ServerGamePacketListenerImpl#onDisconnect` (mensaje de salida) y `#sendPlayerChatMessage` (chat),
`PlayerAdvancements#award` (anuncio de logros). Si un Mixin no se aplica, el juego falla al arrancar (no en silencio).

Sin rastro (`EventosVanish`): los mobs no lo eligen como objetivo (se les quita el objetivo cada 20 ticks en 64 bloques), no recoge ítems del suelo y abre cofres (también dobles y trampa), barriles y shulkers en silencio: sin sonido, sin animación de tapa y sin señal de redstone. Respeta candados, cofre bloqueado y shulker obstruida. Agachado con un ítem en la mano se comporta como vanilla. También el cofre de ender (sin sonido ni tapa). Los mobs con cerebro (Warden, Piglin, Hoglin...) pierden también su objetivo y su ira.

Además: su chat (también `/msg`, `/me`, `/say`, `/teammsg`) llega a los no staff como chat sin firma, sin desconectarlos aunque no lo tengan en TAB; los no staff no oyen sus pasos ni los sonidos/eventos que origina él (`PlayerList#broadcast`); su muerte y sus logros solo se anuncian al staff y a la consola (`ServerPlayer#die`, `PlayerAdvancements#award`); no absorbe esferas de experiencia. Con el panel oculto (`H`) la etiqueta VANISH se sigue mostrando sola.

Límites: las esferas de experiencia aún pueden desviarse hacia el jugador en Vanish (no las absorbe) y las flechas en el suelo pueden recogerse (rastros visibles sin tratar); no oculta de `/list`, selectores (`@a`), placas de presión, cables trampa ni sonidos de bloques. La lista de servidores del menú multijugador (número de jugadores y nombres al pasar el ratón) sigue contando e incluyendo a los jugadores en Vanish.

### Castigar (asistente de menús)

Castigar abre `MenuSelector` en modo `CASTIGAR` (el modo `INVENTARIO` no cambia) y sigue: `MenuElegirCastigo` (3 filas: cabeza + Silenciar/Expulsar/Banear/Congelar/Cárcel/Quitar horas; el lore marca "Activo: quedan X") -> `MenuDuracion` (10 min, 1 h, 1 día, 7 días, Permanente; Quitar horas: 30 min, 1 h, 2 h, Todo el día; Expulsar lo salta) -> `MenuMotivo` (un papel por entrada de `motivos`, paginado; lista vacía = "Otro") -> `MenuConfirmar` (resumen + Aplicar verde / Cancelar rojo). El estado del asistente viaja solo en los constructores. Únicamente Aplicar llama a `Castigos.aplicar`; Volver retrocede un paso y Cancelar vuelve al panel sin aplicar. Elegir a un staff en el selector muestra `esstaff` en la barra y no abre nada; sin cárcel fijada muestra `sincarcel`. Tras aplicar se abre el panel (otro id de ventana) y un flag `aplicado` descarta clics repetidos.

## Castigar: efectos

`EfectosCastigos` (`staff.castigos`) registra los efectos de silenciar, expulsar, banear y quitar horas:

- **Silenciar**: cancela `ServerChatEvent` y los comandos `msg/tell/w/me/say/teammsg/tm` (raíz del comando, también dentro de `/execute ... run`) y avisa con la fecha de fin y el motivo. Solo a no staff. El chat se evalúa en un hilo de trabajo, así que lee una copia segura (`ConcurrentHashMap`) mantenida solo en el hilo del servidor, nunca el registro guardado.
- **Expulsar**: desconecta con el motivo; no deja estado activo (solo historial).
- **Banear**: desconecta al aplicar; al entrar, primero `procesarVencidos` y luego, si el BAN sigue activo, desconecta mostrando la fecha (`yyyy-MM-dd HH:mm`) o "permanente" y el motivo (tomado de la última entrada APLICAR del historial). Límite: el intento de entrar de un baneado muestra brevemente "X se unió" / "X abandonó" a los demás (el aviso de entrada de vanilla ocurre antes de nuestra expulsión).
- **Quitar horas**: normaliza el día, `ReglaTiempo.quitar` (permanente = todo el día), guarda (también sin conexión) y sincroniza; si se agota y el servidor aplica límite, expulsa con el mensaje del límite diario.
- **Vencimientos**: cada 20 ticks y al entrar se llama a `Castigos.procesarVencidos` (avisa "Tu castigo de X terminó" una sola vez y llama a `alQuitar`).

### Congelar y cárcel

`CongelarYCarcel` (`staff.castigos`) registra los efectos de CONGELAR y CARCEL; la geometría pura está en `ReglaPosicion` (radio de cárcel 8 horizontal y vertical, tolerancia del congelado 0.1).

- **Congelar**: al aplicar (conectado y vivo) guarda la posición (`posCongelado`, dimensión + xyz + rotación); si estaba desconectado o muerto se fija donde aparece al entrar o al reaparecer. Cada tick del jugador (servidor), si se movió más de 0.1 o cambió de dimensión, vuelve a su punto (la cabeza gira libre: rotación relativa), se anula la caída y se corta el vuelo con élitros. Cancela romper, colocar, atacar, clic derecho en bloque/ítem, clic izquierdo en bloque, interactuar con entidades, montar, cubos, perlas de ender, fruta coral y portales; cierra cualquier contenedor que se abra. Puede hablar y mirar. Barra de acción "Estás congelado" cada segundo. **No recibe ataques** (`LivingAttackEvent` cancelado: mobs, jugadores, lava, caída...), salvo el daño que ignora la invulnerabilidad (vacío, `/kill`), porque no puede defenderse. Si muere igualmente (sin estar preso), queda congelado donde reaparece (evita un bucle de muertes en un punto letal).
- **Cárcel**: sin cárcel fijada (o con su dimensión desaparecida) `Castigos.aplicar` devuelve `SIN_CARCEL` y no se registra nada. Al aplicar guarda la posición de retorno (`retornoCarcel`; si ya existía, se conserva la original) y lo lleva al centro. Cada tick, fuera del radio 8 o en otra dimensión vuelve al centro ("No puedes salir de la cárcel."). Cancela romper/colocar, cubos (`FillBucketEvent` y clic derecho con cubo: ni vaciar ni recoger agua/lava), perlas, fruta coral, portales y **todos** los comandos ("Estás en la cárcel: no puedes usar comandos."); el chat sí. Al cambiar de dimensión, de vuelta a la cárcel; al reaparecer, en el primer tick (no dentro de `PlayerRespawnEvent`, donde la conexión aún apunta a la entidad vieja). Los traslados del login y de la reaparición no muestran el aviso de fuga; el aviso solo sale si hubo teletransporte. Al terminar (perdón o vencimiento) vuelve al retorno (al spawn del mundo si esa dimensión ya no existe).
- **Desconectado**: un perdón/vencimiento sin conexión deja `retornoCarcel` guardado; al entrar (o en el primer tick/reaparición vivo) se devuelve al jugador y se borra. `alQuitar` es idempotente.
- **Orden en el login**: `EfectosCastigos.alEntrar` llama a `procesarVencidos`, luego al control de ban, y solo después a `CongelarYCarcel.alEntrar`: un castigo vencido nunca se refuerza.
- **Congelado y preso a la vez**: manda la cárcel (el punto congelado se recoloca dentro de la cárcel); el congelado sigue bloqueando acciones. Al salir de la cárcel sigue congelado donde aterriza.
- **Staff**: nunca se teletransporta ni se bloquea (defensivo, aunque no se le pueda castigar).
- Notas: solo se bloquean los comandos que escribe el preso; un `/execute as <preso> run ...` del staff NO se cancela (brigadier conserva la fuente del staff en el contexto raíz). Un preso o congelado no puede ser movido de dimensión por el staff (el viaje se cancela) y un `/tp` en la misma dimensión se deshace en el siguiente tick: perdonar primero.

## Estructura (`com.senju.core`)

- `config`: `CommonConfig`, `ClientConfig`.
- `tiempo`: `ReglaTiempo` (lógica pura), `EstadoTiempo`, `RegistroTiempo` (guardado), `ControlTiempo` (eventos del servidor), `ComandoTiempo`.
- `network`: `Red`, `PaqueteTiempo`, `PaqueteInfoServidor`.
- `cliente`: `PanelHud`, `Teclas`, `EventosCliente`, `DatosCliente`, `Posicion`, `Formato`.
- `staff`: `Staff`, `ReglaStaff` (lógica pura), `ItemsMod`, `PlacaStaffItem`, `ProteccionPlaca`, `ComandoStaff`.
- `staff.menu`: `AccionMenu`, `Paginacion`, `DisposicionInventario` (lógica pura), `MenuSoloLectura` (base), `MenuStaff`, `MenuSelector`, `MenuVerInventario`, `MenuVerEnder`, `MenuProhibidos` (menús), `BotonesMenu`, `CopiaVista` (ítems).
- `staff.prohibidos`: `ListaProhibidos` (lógica pura), `RegistroProhibidos` (guardado), `Prohibidos` (fachada), `Confiscacion`, `RegistroConfiscados` (log de confiscaciones).
- `staff.castigos`: `TipoCastigo`, `Duracion`, `EstadoCastigos`, `ReglaPosicion` (lógica pura), `RegistroCastigos` (guardado), `RegistroLogCastigos`, `Castigos` (servicio), `EfectoCastigo`, `EfectosCastigos`, `CongelarYCarcel` (efectos).
- `staff.vanish`: `ReglaVanish` (lógica pura), `RegistroVanish` (guardado), `Vanish` (estado, visibilidad, eventos).
- `mixin`: `ServerPlayerMixin`, `PlayerListMixin`, `ServerGamePacketListenerImplMixin` (ocultación de Vanish).

Tests unitarios (JUnit): `ReglaTiempoTest`, `PosicionTest`, `FormatoTest`, `HumoTest`, `EfectosTest`, `ReglaStaffTest`, `ReglaVanishTest`, `AccionMenuTest`, `PaginacionTest`, `DisposicionInventarioTest`, `ListaProhibidosTest`, `DuracionTest`, `EstadoCastigosTest`, `ReglaPosicionTest` (82 en total).

## Pruebas dentro del juego (checklist)

Prueba rápida del límite: en `run/config/senjucore-common.toml` pon `horasDiarias = 0.05` (3 minutos).
Arranca `runServer` y `runClient` y entra a `localhost`.

> **Importante (solo para pruebas):** el cliente de desarrollo entra como `Dev`, sin cuenta de
> Mojang. En `run/server.properties` pon `online-mode=false` y `enforce-secure-profile=false`,
> o el servidor te rechazará con "Failed to verify username". No uses esto en el servidor real.
>
> Si al detener el juego desde IntelliJ ves `Build cancelled while executing task ...BootstrapLauncher.main()`,
> no es un error: solo significa que la ejecución se detuvo con el botón de stop.

- [ ] Panel: con `nombreServidor = "Senju Test"` aparecen nombre del servidor, cara y nombre, tiempo restante, hora (12 h) y "Misiones: Próximamente".
- [ ] `J` recorre las 6 posiciones (incluidas medio-izq y medio-der) sin salirse de la pantalla, también con GUI scale 4.
- [ ] `H` oculta y muestra el panel; al reiniciar el cliente se conservan posición y visibilidad.
- [ ] F1 oculta el panel; abrir el inventario lo oculta; el chat lo mantiene.
- [ ] Con `horasDiarias = 0.5` el tiempo se pone amarillo (< 30 min) y luego rojo (< 10 min).
- [ ] Con `horasDiarias = 0.05`: aviso "Te quedan 1 min de juego hoy." hacia los 2 min y expulsión a los 3 min con el mensaje.
- [ ] Reconectar el mismo día: te rechaza al instante. Reiniciar el servidor y reconectar: sigue rechazando (persistencia).
- [ ] Tras la medianoche del servidor se puede entrar con el tiempo completo.
- [ ] Como operador (`op <nombre>` en la consola): `/tiempo ver`, `/tiempo dar <jugador> 5` (sube 5 min y el HUD se actualiza), `/tiempo reiniciar` (restante = límite completo).
- [ ] `/tiempo reiniciar <jugador>` sobre un jugador ya expulsado → puede volver a entrar.
- [ ] Con F3 abierto el panel se oculta.
- [ ] En ABAJO_IZQ/ABAJO_DER con la ventana por defecto (854x480) el panel no tapa la barra rápida ni los corazones.
- [ ] Un jugador no operador recibe "comando desconocido" con `/tiempo`.
- [ ] Un jugador: crear mundo nuevo; el panel muestra el nombre del mundo y "Tiempo jugado hoy" creciendo; nunca te expulsa.
- [ ] Las teclas aparecen en Controles bajo "Senju Core".
- [ ] Efectos: la fuente Tektur se ve legible en el panel con escala 1, 2 y 3 (GUI scale).
- [ ] Efectos: el nombre del servidor se escribe, hace pausa, se borra y repite, con cursor `_` parpadeante; el ancho del panel no cambia.
- [ ] Efectos: con `rgb = true` los colores cambian de forma continua (letra por letra en el servidor, por línea en el resto).
- [ ] Efectos: con `horasDiarias = 0.15` la línea de tiempo parpadea cuando quedan menos de 10 min.
- [ ] Efectos: con `rgb = false` vuelven los colores clásicos (blanco, tiempo blanco/amarillo/rojo, misiones gris) y no hay parpadeo.
- [ ] Efectos: con `fuentePersonalizada = false` el panel vuelve a la fuente de Minecraft.

### Placa de Staff

- [ ] Tirar con Q (también Ctrl+Q): la placa se queda en el inventario.
- [ ] Q: la placa sigue visible en la mano.
- [ ] Arrastrarla fuera de la ventana del inventario (y desde creativo): vuelve al inventario.
- [ ] Meterla en un cofre (clic, shift+clic, arrastre): vuelve al inventario al instante.
- [ ] No se puede meter en una shulker (clic, shift-clic, tolva).
- [ ] Tolva bajo un cofre: no se puede sacar por la tolva; shulker box: igual.
- [ ] Clic derecho con la placa en un marco y en un soporte de armadura: no pasa nada.
- [ ] Morir con la placa (con y sin `keepInventory`): reaparece con una sola placa, sin ítem en el suelo.
- [ ] Inventario lleno y placa en un cofre: al cerrar el cofre queda pendiente y en el siguiente tick aparece en el cursor (o en un hueco al liberarlo).
- [ ] `/give` a un jugador no staff: desaparece en <= 1 s con el aviso.
- [ ] Quitar a alguien de `staff` / hacerle deop: pierde la placa (también en mano secundaria y cursor).
- [ ] Consola: `/staff placa <jugador>` funciona.

### Menú del staff
- [ ] Clic derecho con la Placa abre "Panel de Staff"; un no staff con una placa (obtenida a escondidas) no abre nada.
- [ ] Cerrar cierra; clic en cristal gris no hace nada (sin tooltip).
- [ ] Clic en la poción alterna Vanish: cambia nombre (activado/desactivado) y brillo al instante; el panel muestra/oculta VANISH. Tooltip sin texto de efecto.
- [ ] Ningún clic mueve ítems: shift+clic (en menú e inventario propio), arrastre (clic mantenido), teclas 1-9, F (mano secundaria), Q (soltar), doble clic, clic central (creativo), clic fuera de la ventana. Cursor siempre vacío.
- [ ] Con el menú abierto, `deop` al jugador (si no está en `staff`): el menú se cierra.
- [ ] Cerrar con Esc/E no devuelve ni pierde nada; la Placa sigue intacta en el inventario.

### Revisar inventario (hacen falta dos jugadores, S staff y N normal)
- [ ] S: Panel > Revisar inventario abre "Elegir jugador" con la cabeza (skin) de cada jugador conectado, por orden alfabético; Volver regresa al Panel. Entra/sale N: la lista se actualiza en <= 1 s.
- [ ] Con 46+ conectados (o bots): dos páginas; Siguiente/Anterior solo aparecen cuando existen; volver desde una vista regresa a la misma página.
- [ ] Clic en la cabeza de N: "Inventario de N" con su inventario, armadura (36-39) y mano secundaria (40) en su sitio. Mover/gastar ítems en N se refleja en <= 1 s sin ítems fantasma; Actualizar refresca al instante.
- [ ] Ningún clic mueve ítems en estos menús (shift, arrastre, teclas 1-9, F, Q, doble clic); el cursor queda vacío y los originales de N no cambian.
- [ ] Un ítem prohibido en N muestra la línea roja "Objeto prohibido" solo en la vista (el original de N no la tiene).
- [ ] Cofre de ender: muestra el contenido en vivo; Volver regresa a la vista de inventario.
- [ ] N se desconecta, muere o cambia de dimensión con la vista abierta: sin errores en el log; si se desconecta, mensaje "N se desconectó." y vuelve al selector.
- [ ] S ve a S mismo en el selector: su Placa aparece como papel "Placa de Staff"; cerrar el menú no duplica la Placa. Una Placa en el inventario de otro staff también se ve como papel.
- [ ] `deop` a S con la vista abierta: el menú se cierra.

### Objetos prohibidos (S staff; para la confiscación, N normal)
- [ ] Panel > Objetos prohibidos (libro y pluma) abre "Objetos prohibidos" vacío; Volver regresa al Panel.
- [ ] **Diamante:** S toma 5 diamantes con el cursor y hace clic en una casilla vacía: aparece un diamante con "Clic para permitir de nuevo", todo el staff ve "S prohibió: Diamante" y S **sigue teniendo los 5 diamantes en el cursor**. Clic derecho: igual. Cerrar con Esc: los 5 diamantes vuelven al inventario.
- [ ] Shift+clic sobre un diamante (o cualquier ítem) del inventario de S: se agrega a la lista y el ítem no se mueve.
- [ ] Clic con el cursor vacío sobre el diamante de la lista: desaparece, "S permitió: Diamante".
- [ ] Con un ítem en el cursor, clic en Volver/Anterior/Siguiente/cristal: el ítem no se pierde (Volver lo devuelve al inventario al abrir el Panel).
- [ ] S mueve sus propios ítems con clic, teclas 1-9, F y doble clic dentro de su inventario; nada entra ni sale de las casillas de la lista con: clic, shift+clic, arrastre (clic mantenido) sobre la lista o el inventario, teclas 1-9 y F sobre una casilla de la lista, doble clic sobre la lista, clic central (creativo), clic fuera de la ventana y Q/Ctrl+Q sobre su inventario (no suelta nada).
- [ ] Agregar de nuevo un tipo ya listado: "Diamante ya está prohibido." en la barra de acción, sin aviso al staff.
- [ ] Intentar agregar la Placa (clic con ella en el cursor y shift+clic): "La Placa de Staff no se puede prohibir." y la Placa sigue intacta.
- [ ] 46 tipos prohibidos: dos páginas; Siguiente/Anterior solo cuando existen; quitar el único ítem de la página 2 regresa a la página 1.
- [ ] Dos staff con el menú abierto: lo que agrega uno aparece al otro en <= 1 s. Reiniciar el servidor: la lista se conserva.
- [ ] Staff en modo espectador: abre el menú pero los clics no hacen nada (limitación vanilla).
- [ ] N con diamantes en el inventario, armadura/mano secundaria, cursor y cofre de ender: se le quitan en <= 1 s, sin ítems en el suelo, con aviso a N y al staff; línea en `logs/senjucore-confiscados.log`. S (staff) conserva sus diamantes.
- [ ] N sale del juego con un diamante en el cursor (menú abierto): al reentrar no hay diamante y no hay ítem en el suelo; el log tiene origen `cursor`.
- [ ] N pone un diamante en la rejilla de crafteo 2x2 (o en un yunque) y cierra o sale: se confisca (origen `crafteo`), sin ítem en el suelo.
- [ ] N intenta tirar un diamante con Q (o arrastrándolo fuera): desaparece sin entidad en el suelo, con aviso y línea `tirado` en el log.
- [ ] N muere con un diamante (p. ej. recogido justo antes): no aparece en los drops; línea `muerte` en el log. Con una shulker box prohibida con cosas dentro: se borra todo y el log solo menciona la shulker.

### Vanish (hacen falta dos jugadores)

El entorno de desarrollo abre un solo cliente. Para ver lo que ve un **no staff** necesitas un segundo
cliente: otra instancia de Minecraft (otro launcher/cuenta, o una segunda copia de `runClient` con otro
nombre si `online-mode=false`) o probar en el servidor real con un amigo. Llamaremos **S** al staff (op) y **N** al no staff.

- [ ] S: `/staff vanish` → "Vanish activado", línea VANISH en el panel. N deja de ver a S y S desaparece de su TAB.
- [ ] S: `/staff vanish` otra vez → N vuelve a ver a S (entidad y TAB) sin reconectar.
- [ ] Con S en Vanish, N sale y vuelve a entrar: N nunca ve a S (ni un instante en TAB ni la entidad).
- [ ] S en Vanish sale y vuelve a entrar: N no ve "S abandonó"/"S se unió" ni a S en TAB; otro staff sí ve los mensajes.
- [ ] Con S en Vanish, hacer op a N (`op N`): en <= 1 s N ve a S (entidad y TAB). `deop N`: en <= 1 s deja de verlo.
- [ ] S en Vanish y `deop S` (si `staff` no lo incluye): en <= 1 s se desactiva Vanish y N lo ve.
- [ ] Reiniciar el servidor con S en Vanish: al entrar S sigue en Vanish y N no lo ve.
- [ ] S en Vanish abre cofre simple, cofre doble, cofre trampa, barril y shulker: N (cerca) no oye ni ve la tapa abrirse; el contenido se puede mover y se guarda.
- [ ] S en Vanish con cofre bajo un bloque sólido (o gato sentado) o shulker obstruida: no abre, igual que vanilla. Contenedor con candado: muestra el mensaje y no abre.
- [ ] S en Vanish agachado con un bloque en la mano sobre un cofre: coloca el bloque (vanilla). Romper el cofre con el menú abierto lo cierra.
- [ ] Un zombi/esqueleto cercano deja de perseguir a S en <= 1 s; S no recoge ítems del suelo. Al salir de Vanish todo vuelve a la normalidad.
- [ ] S en Vanish escribe en el chat y usa `/msg N hola`: N no es desconectado y ve los mensajes (sin entrada de S en TAB). Igual con `/me` y `/say`.
- [ ] S en Vanish camina y corre junto a N: N no oye sus pasos (los sonidos que vanilla emite sin origen, p. ej. comer, sí se oyen).
- [ ] S en Vanish oculta el panel con `H`: la etiqueta VANISH sigue visible sola en la esquina del panel (F1 y F3 la ocultan).
- [ ] S en Vanish muere (`/kill`) y consigue un logro: N no ve el mensaje de muerte ni el anuncio del logro; otro staff y la consola sí.
- [ ] S en Vanish junto a esferas de experiencia: no las absorbe (pueden acercarse); al salir de Vanish las recoge.

### Castigos: efectos (S staff, N no staff; aplicar desde el panel o con `/staff banear`)
- [ ] N silenciado 1 h: escribe en el chat y usa `/msg S hola`, `/me x`, `/say x`: no sale nada y N ve "Estás silenciado hasta <fecha>. Motivo: ...". S silenciado no queda afectado.
- [ ] N silenciado 1 m: al pasar el minuto N ve "Tu castigo de Silenciar terminó." (una sola vez) y puede escribir.
- [ ] Expulsar a N: se desconecta con el motivo y puede volver a entrar de inmediato.
- [ ] Banear a N 1 h (conectado): se desconecta con "baneado hasta <fecha>"; al reintentar entrar lo vuelve a expulsar con fecha y motivo. Banear permanente: dice "permanente".
- [ ] Banear a N (desconectado) y que entre: expulsado al entrar. Ban de 1 m vencido estando desconectado: al entrar puede jugar y ve "Tu castigo de Banear terminó.".
- [ ] Quitar 30 m a N (con límite activo en servidor dedicado): el panel baja 30 m. "Todo el día" lo expulsa con el mensaje del límite. Con N desconectado se aplica al entrar.

### Castigar: menús (S staff, N no staff, T otro staff)
- [ ] Panel > Castigar abre el selector; clic en la cabeza de T: "El staff no puede ser castigado" en la barra y no abre nada. Revisar inventario sigue abriendo el inventario.
- [ ] Clic en N: 6 botones; uno ya activo muestra "Activo: quedan ..." y se actualiza.
- [ ] Silenciar > 10 min > motivo > Confirmar muestra cabeza "Castigar a N" con castigo, duración y motivo; Aplicar lo aplica y vuelve al panel; doble clic rápido en Aplicar aplica una sola vez (un registro en el historial).
- [ ] Expulsar salta la duración; Quitar horas ofrece 30 min / 1 h / 2 h / Todo el día y el resumen dice "todo el día".
- [ ] Volver en cada paso retrocede un paso; Cancelar vuelve al panel y no aplica nada (comprobar `/staff historial` o el log).
- [ ] Cárcel sin fijar: Aplicar muestra "No hay cárcel fijada" y no registra nada.
- [ ] Con más de 45 motivos en la config aparecen las flechas de página; con la lista vacía hay un único "Otro".

### Congelar y cárcel (S staff, N no staff)
- [ ] Congelar N 10 m: N no puede andar, saltar, agacharse para bajar ni volar: vuelve a su punto; puede girar la cabeza y hablar. Barra "Estás congelado" cada segundo.
- [ ] N congelado: romper, colocar, atacar, abrir cofre/horno/caballo/aldeano, usar ítems (comer, arco, cubo): nada pasa; si llega a abrirse un contenedor se cierra.
- [ ] N congelado: lanzar perla de ender y comer fruta coral: no se mueve.
- [ ] N congelado montado en caballo/barco: queda desmontado; no puede volver a montar.
- [ ] N congelado en un portal del Nether/End: no cambia de dimensión.
- [ ] N congelado volando con élitros/cayendo desde altura: vuelve al punto sin daño de caída.
- [ ] N congelado: un zombi, S con espada, lava o caída no le hacen daño.
- [ ] S usa `/kill N` con N congelado (no preso): reaparece en su cama/spawn y queda congelado allí (sin aviso de fuga).
- [ ] N congelado se desconecta y vuelve: sigue congelado en el mismo punto.
- [ ] Congelar a N desconectado; al entrar queda congelado donde aparece. Congelar a N en la pantalla de muerte: queda congelado donde reaparece.
- [ ] Congelar 1 m: al vencer puede moverse ("Tu castigo de Congelar terminó."). Perdonar: igual.
- [ ] Cárcel sin `/staff carcel fijar`: "No hay cárcel fijada..." y nada en el historial/log.
- [ ] Cárcel fijada; encarcelar a N 10 m: N aparece en el centro. Caminar > 8 bloques (o caer/subir > 8): vuelve al centro con "No puedes salir de la cárcel.".
- [ ] N preso: `/spawn`, `/home`, `/tp`, `/kill`, `/msg`, `/execute ...`: todos cancelados con "Estás en la cárcel: no puedes usar comandos."; el chat normal sí funciona.
- [ ] N preso: cubo de agua/lava (vaciar y recoger) no funciona; S con `/execute as N run say hola` sí funciona.
- [ ] N preso: romper/colocar bloques no funciona; perla de ender y fruta coral no lo sacan; portal (si hubiera) no lo cambia de dimensión.
- [ ] N preso muere (lava, vacío, `/kill N` por S): reaparece en la cárcel aunque tenga cama (el cliente ve la cárcel, no la cama), sin "No puedes salir de la cárcel.".
- [ ] Encarcelar a N en la pantalla de muerte: al reaparecer va a la cárcel; al terminar vuelve a donde reapareció.
- [ ] Perdonar la cárcel con N en la pantalla de muerte: al reaparecer queda en su posición anterior a la cárcel.
- [ ] S lo cambia de dimensión (`/execute in minecraft:the_nether run tp N ...`): no se mueve o vuelve a la cárcel al instante.
- [ ] N preso se desconecta y vuelve: sigue en la cárcel.
- [ ] Encarcelar de nuevo a N ya preso (otra duración): sigue en la cárcel y al terminar vuelve a donde estaba ANTES del primer encarcelamiento.
- [ ] Perdonar la cárcel con N conectado: vuelve a su posición anterior (dimensión incluida, con su rotación).
- [ ] Cárcel de 1 m que vence con N desconectado (o perdón con N desconectado): al entrar aparece en su posición anterior, no en la cárcel, y ve "Tu castigo de Cárcel terminó." (si venció).
- [ ] Encarcelar a N desconectado: al entrar va a la cárcel; al terminar vuelve a donde había entrado.
- [ ] N congelado y preso: queda en la cárcel, congelado. Perdonar solo la cárcel: vuelve a su posición y sigue congelado allí.
- [ ] Reiniciar el servidor con N preso / congelado: el estado sigue al entrar.

### Castigar: comandos y recorrido completo (S staff, N no staff, T otro staff)
- [ ] Un no staff no ve ni puede ejecutar `/staff perdonar|historial|carcel|banear` (sin permiso).
- [ ] `/staff carcel fijar` desde la consola: error de "solo jugador". Como S: "Cárcel fijada en <dimensión> (x, y, z)"; reiniciar el servidor conserva la cárcel.
- [ ] `/staff banear N 10m Spam` (N conectado): N se desconecta con fecha y motivo; el staff conectado ve el aviso; `/staff historial N` lo lista.
- [ ] `/staff banear Fantasma 1h Grief` con un jugador desconectado (o desde la consola): lo banea; al entrar lo rechaza. Con `perm` dice "permanente".
- [ ] `/staff banear N 5x algo` y `/staff banear N 0m algo`: "Duración inválida" con ejemplos. Banear a T (staff): "El staff no puede ser castigado".
- [ ] `/staff perdonar N` con varios castigos: quita todos ("Perdonado: N"); repetir: "N no tenía castigos activos". `/staff perdonar N ban` solo quita el ban. Funciona con N desconectado.
- [ ] `/staff perdonar N expulsion` no existe como opción (autocompletado solo silencio, ban, congelar, carcel).
- [ ] `/staff historial N` muestra aplicar, perdonar y vencer, del más reciente, máximo 10; sin registros: "Sin historial". Texto en español, igual con idioma inglés.
- [ ] Recorrido completo: fijar cárcel, castigar a N desde el panel con cada uno de los 6 castigos, verificar efecto, perdonar con el comando, revisar `logs/senjucore-castigos.log` (una línea por acción, sin saltos raros en motivos).
- [ ] Todo sobrevive a reiniciar el servidor (silencio, ban, congelado, cárcel, historial).

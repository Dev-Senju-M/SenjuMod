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

Clic derecho con la Placa abre "Panel de Staff" (cofre de 3 filas; `MenuStaff` sobre `MenuType.GENERIC_9x3`, el cliente usa la pantalla vanilla). Botones: Castigar (slot 10) e Inventario (16) muestran "Próximamente" en la barra de acción; Vanish (13, poción: brilla si está activo) alterna Vanish; Cerrar (22). `clicked` nunca llama a `super`: ningún tipo de clic mueve ítems ni deja nada en el cursor; solo un clic simple (PICKUP) en un botón actúa. Se cierra solo si pierdes staff. Los botones nunca son placas (compatible con `ProteccionPlaca`).

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

## Estructura (`com.senju.core`)

- `config`: `CommonConfig`, `ClientConfig`.
- `tiempo`: `ReglaTiempo` (lógica pura), `EstadoTiempo`, `RegistroTiempo` (guardado), `ControlTiempo` (eventos del servidor), `ComandoTiempo`.
- `network`: `Red`, `PaqueteTiempo`, `PaqueteInfoServidor`.
- `cliente`: `PanelHud`, `Teclas`, `EventosCliente`, `DatosCliente`, `Posicion`, `Formato`.
- `staff`: `Staff`, `ReglaStaff` (lógica pura), `ItemsMod`, `PlacaStaffItem`, `ProteccionPlaca`, `ComandoStaff`.
- `staff.menu`: `AccionMenu` (lógica pura), `MenuStaff` (menú), `BotonesMenu` (ítems).
- `staff.vanish`: `ReglaVanish` (lógica pura), `RegistroVanish` (guardado), `Vanish` (estado, visibilidad, eventos).
- `mixin`: `ServerPlayerMixin`, `PlayerListMixin`, `ServerGamePacketListenerImplMixin` (ocultación de Vanish).

Tests unitarios (JUnit): `ReglaTiempoTest`, `PosicionTest`, `FormatoTest`, `HumoTest`, `EfectosTest`, `ReglaStaffTest`, `ReglaVanishTest`, `AccionMenuTest` (46 en total).

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
- [ ] Clic izquierdo y derecho en Castigar/Inventario: "Próximamente" en la barra; Cerrar cierra; clic en cristal gris no hace nada (sin tooltip).
- [ ] Clic en la poción alterna Vanish: cambia nombre (activado/desactivado) y brillo al instante; el panel muestra/oculta VANISH. Tooltip sin texto de efecto.
- [ ] Ningún clic mueve ítems: shift+clic (en menú e inventario propio), arrastre (clic mantenido), teclas 1-9, F (mano secundaria), Q (soltar), doble clic, clic central (creativo), clic fuera de la ventana. Cursor siempre vacío.
- [ ] Con el menú abierto, `deop` al jugador (si no está en `staff`): el menú se cierra.
- [ ] Cerrar con Esc/E no devuelve ni pierde nada; la Placa sigue intacta en el inventario.

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

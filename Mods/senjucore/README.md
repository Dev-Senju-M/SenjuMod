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

Ítem `senjucore:placa_staff` exclusivo del staff (por ahora, clic derecho muestra "Menú del staff: próximamente").
Es staff quien sea operador nivel 2+ (si `staffIncluyeOperadores = true`) o esté en la lista `staff`
(nombres, sin distinguir mayúsculas) de `senjucore-common.toml`. Se obtiene con `/staff placa [jugador]`
o desde la pestaña creativa de operadores.

Protección (servidor): no se tira (Q, arrastrar fuera, creativo), no se queda en contenedores, marcos ni
soportes ni en un Allay (clic derecho bloqueado), se conserva al morir, y a quien no es staff se le elimina (revisión cada 1 s) con aviso.
Si no cabe en el inventario queda pendiente en memoria y el siguiente tick del jugador la pone en un
hueco libre o, si no lo hay, en el cursor cuando está vacío; si no, espera (nunca se suelta al suelo). Las placas apartadas al morir viven en memoria: si el
servidor se reinicia con un jugador muerto sin reaparecer, se pierden, igual que las guardadas para un jugador desconectado (`pendientes`); se recupera con `/staff placa`.

Limitación: en modo creativo, la papelera o la cuadrícula de ítems del inventario creativo pueden
borrar la placa (es del cliente); se recupera con `/staff placa`. Los contenedores se revisan al inicio
de cada tick del servidor (antes que las tolvas) y, si un no staff ve la placa en un contenedor, se descarta.

## Estructura (`com.senju.core`)

- `config`: `CommonConfig`, `ClientConfig`.
- `tiempo`: `ReglaTiempo` (lógica pura), `EstadoTiempo`, `RegistroTiempo` (guardado), `ControlTiempo` (eventos del servidor), `ComandoTiempo`.
- `network`: `Red`, `PaqueteTiempo`, `PaqueteInfoServidor`.
- `cliente`: `PanelHud`, `Teclas`, `EventosCliente`, `DatosCliente`, `Posicion`, `Formato`.

Tests unitarios (JUnit): `ReglaTiempoTest`, `PosicionTest`, `FormatoTest`, `HumoTest` (23 en total).

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
- [ ] Arrastrarla fuera de la ventana del inventario (y desde creativo): vuelve al inventario.
- [ ] Meterla en un cofre (clic, shift+clic, arrastre): vuelve al inventario al instante.
- [ ] Tolva bajo un cofre: no se puede sacar por la tolva; shulker box: igual.
- [ ] Clic derecho con la placa en un marco y en un soporte de armadura: no pasa nada.
- [ ] Morir con la placa (con y sin `keepInventory`): reaparece con una sola placa, sin ítem en el suelo.
- [ ] Inventario lleno y placa en un cofre: al cerrar el cofre queda pendiente y en el siguiente tick aparece en el cursor (o en un hueco al liberarlo).
- [ ] `/give` a un jugador no staff: desaparece en <= 1 s con el aviso.
- [ ] Quitar a alguien de `staff` / hacerle deop: pierde la placa (también en mano secundaria y cursor).
- [ ] Consola: `/staff placa <jugador>` funciona.

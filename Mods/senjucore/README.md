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

## Estructura (`com.senju.core`)

- `config`: `CommonConfig`, `ClientConfig`.
- `tiempo`: `ReglaTiempo` (lógica pura), `EstadoTiempo`, `RegistroTiempo` (guardado), `ControlTiempo` (eventos del servidor), `ComandoTiempo`.
- `network`: `Red`, `PaqueteTiempo`, `PaqueteInfoServidor`.
- `cliente`: `PanelHud`, `Teclas`, `EventosCliente`, `DatosCliente`, `Posicion`, `Formato`.

Tests unitarios (JUnit): `ReglaTiempoTest`, `PosicionTest`, `FormatoTest`, `HumoTest` (23 en total).

## Pruebas dentro del juego (checklist)

Prueba rápida del límite: en `run/config/senjucore-common.toml` pon `horasDiarias = 0.05` (3 minutos).
Arranca `runServer` y `runClient` y entra a `localhost`.

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

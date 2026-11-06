# Diseño: Panel del jugador y límite de tiempo diario (Forge 1.20.1)

Fecha: 2026-11-06
Estado: borrador para revisión

## 1. Intención

Un mod para un servidor de Minecraft Java Edition que:

1. Muestra a cada jugador un **panel fijo en pantalla (HUD)** con sus datos.
2. Aplica un **límite de tiempo de juego diario** por jugador: al agotarse, el servidor lo
   saca y no lo deja volver hasta que cambia el día en la vida real.
3. Sirve de **base para un sistema de Misiones** futuro (por eso el mod va en servidor y
   cliente, con canal de red propio desde el principio).

## 2. Decisiones del usuario

- Plataforma: **Minecraft Java Edition, Forge 1.20.1**. IDE: **IntelliJ IDEA**.
- Ubicación: `Minecraft/Mods/` dentro de este repositorio.
- El panel es un **HUD siempre visible**, solo lo ve cada jugador para sí mismo.
- La **posición del panel es configurable**: esquinas y también el medio vertical del
  lado izquierdo o derecho.
- Funciona en **individual y en servidor**.
- El mod es **obligatorio en servidor y cliente**.
- Datos del panel (por ahora):
  - **Nombre oficial del servidor** (definido por el administrador).
  - **Nombre del jugador con su personaje** (cara de la skin).
  - **Tiempo** → es el tiempo diario restante del límite (ver §4).
  - **Hora real** en formato **12 h** (ej. `9:45 PM`).
  - **Misiones**: sección reservada para el futuro.
- Límite diario: **6 horas** para empezar. Al agotarse → expulsión y bloqueo hasta el
  cambio de día real.
- Aceptado de las propuestas: tecla para ocultar/mostrar, posición guardada en
  configuración, el tiempo solo corre conectado, reinicio a medianoche según el reloj del
  servidor, avisos antes de expulsar, sin límite en individual.

## 3. Objetivos, no‑objetivos y anti‑objetivos

**Objetivos**
- Panel legible, discreto, que no tape la barra de vida/inventario en ninguna esquina.
- Límite diario confiable: el conteo lo lleva el **servidor**, no el cliente.
- Datos persistentes: reiniciar el servidor no borra el tiempo usado del día.
- Código organizado para añadir Misiones sin rehacer red ni HUD.

**No‑objetivos (por ahora)**
- Sistema de misiones (solo el hueco visual "Misiones: Próximamente").
- Límites distintos por jugador o rangos/VIP.
- Editor visual arrastrando el panel con el ratón.
- Soporte para Fabric/NeoForge u otras versiones.

**Anti‑objetivos (sería un fracaso aunque “funcione”)**
- Que un jugador pueda saltarse el límite (desconectarse/reconectarse, cambiar la hora de
  su PC, modificar su cliente).
- Que el panel tape elementos importantes de la interfaz o baje los FPS.
- Perder el tiempo usado de los jugadores al reiniciar el servidor.

## 4. Comportamiento

### 4.1 Panel (cliente)

Contenido, de arriba a abajo:

```
 Mi Servidor                       ← nombre oficial
 [cara] Steve                      ← cabeza de la skin + nombre
 Tiempo restante: 5h 32m
 Hora: 9:45 PM
 Misiones: Próximamente
```

- Fondo semitransparente, texto blanco; el tiempo restante cambia a amarillo con
  < 30 min y a rojo con < 10 min. *(mi decisión)*
- **Tecla para mostrar/ocultar**: `H` por defecto, reasignable en Controles. *(mi decisión)*
- **6 posiciones posibles** (pedido del usuario: también al medio de la pantalla en
  vertical, a la izquierda o a la derecha):
  arriba‑izq, medio‑izq, abajo‑izq, arriba‑der, medio‑der, abajo‑der.
  "Medio" = centrado verticalmente en el borde de ese lado.
- **Tecla para cambiar de posición**: `J` por defecto, cicla
  arriba‑izq → medio‑izq → abajo‑izq → arriba‑der → medio‑der → abajo‑der. *(mi decisión)*
- Configuración de cliente (`config/senjucore-client.toml`): `visible`, `posicion`,
  `desplazamientoX`, `desplazamientoY`, `escala`. Los cambios por tecla se guardan aquí.
- El panel se oculta con F1 (como el resto del HUD) y cuando hay una pantalla abierta que
  lo taparía (chat no cuenta). *(mi decisión)*
- La hora real se toma del reloj de la PC del jugador (es solo informativa).
- El tiempo restante se descuenta localmente cada segundo para que se vea fluido, y se
  corrige con el valor del servidor en cada sincronización.

**En individual**: no hay límite. La línea de tiempo muestra
`Tiempo jugado hoy: 1h 05m` y el nombre del servidor muestra el nombre del mundo.

### 4.2 Límite diario (servidor)

- Cada jugador (por UUID) tiene un registro `{día, segundosUsados}`.
- Mientras está conectado, el servidor suma 1 segundo cada 20 ticks.
- **Cambio de día**: cuando la fecha local del servidor cambia (medianoche), el registro se
  reinicia a 0. Esto ocurre también para jugadores conectados en ese momento.
- **Avisos** por chat y en el panel al quedar 10 min y 1 min. (Configurable.)
- **Al llegar a 0**: el servidor desconecta al jugador con el mensaje
  "Se acabó tu tiempo de hoy. Vuelve después de la medianoche (hora del servidor)."
- **Al intentar entrar sin tiempo**: se le desconecta en cuanto entra, con el mismo mensaje.
- Persistencia: datos guardados en el mundo (`SavedData` del Overworld), se guardan con el
  mundo y al apagar el servidor.
- Operadores: **sí tienen límite** por defecto; opción `eximirOperadores` en la config. *(mi decisión)*

### 4.3 Comandos de administrador *(mi decisión — necesarios para gestionar el límite)*

Requieren nivel de permiso 2 (operador):
- `/tiempo ver <jugador>` → tiempo usado y restante hoy.
- `/tiempo reiniciar <jugador>` → pone su tiempo usado de hoy en 0.
- `/tiempo dar <jugador> <minutos>` → resta minutos a lo usado (le da tiempo extra hoy).

### 4.4 Configuración del servidor

`config/senjucore-common.toml` (no por mundo, para que sea fácil de editar):

| Clave | Por defecto | Descripción |
|---|---|---|
| `nombreServidor` | `"Mi Servidor"` | Nombre oficial que ve cada jugador |
| `horasDiarias` | `6.0` | Límite diario en horas |
| `avisosMinutos` | `[10, 1]` | Minutos restantes en los que se avisa |
| `eximirOperadores` | `false` | Si los OP no tienen límite |

## 5. Arquitectura

Mod único: **`Senju Core`**, id `senjucore`, paquete `com.senju.core`. *(mi decisión; se
puede renombrar antes de empezar)*. Pensado para crecer con Misiones.

```
Minecraft/Mods/senjucore/
  build.gradle, settings.gradle, gradle.properties, gradlew(.bat)
  src/main/java/com/senju/core/
    SenjuCore.java              // @Mod, registro de config, red y eventos
    config/ CommonConfig.java, ClientConfig.java
    network/ Red.java           // SimpleChannel "senjucore:main", versión de protocolo
             PaqueteInfoServidor.java   // S2C: nombreServidor, limiteSegundos, esIndividual
             PaqueteTiempo.java         // S2C: segundosRestantes, segundosUsadosHoy
    tiempo/ ReglaTiempo.java    // LÓGICA PURA (sin clases de Minecraft): sumar, reinicio de
                                //   día, ¿agotado?, ¿toca aviso?
            RegistroTiempo.java // SavedData: Map<UUID, {epochDay, usados}>
            ControlTiempo.java  // eventos: tick, login, logout, envío de paquetes, expulsión
            ComandoTiempo.java
    cliente/ PanelHud.java      // IGuiOverlay registrado en RegisterGuiOverlaysEvent
             DatosCliente.java  // estado recibido + cuenta regresiva local
             Teclas.java
             Formato.java       // LÓGICA PURA: "5h 32m", "9:45 PM"
  src/main/resources/META-INF/mods.toml
  src/main/resources/assets/senjucore/lang/es_es.json, en_us.json
  src/test/java/...             // JUnit 5 para ReglaTiempo y Formato
```

**Flujo de red**
- Al entrar: servidor envía `PaqueteInfoServidor` y `PaqueteTiempo`.
- Cada 30 s y en cada aviso/reinicio/comando: servidor envía `PaqueteTiempo`.
- El canal declara versión; un cliente sin el mod (o con otra versión) no puede entrar al
  servidor, y Forge lo indica en pantalla. Futuros paquetes de Misiones irán en este canal.

**Seguridad del límite**: el cliente nunca envía tiempo; solo lo recibe. La fecha la decide
el servidor, así que cambiar la hora de la PC no sirve de nada.

## 6. Entorno y herramientas

- Java **17** (JDK Temurin 17), ForgeGradle 6, Forge **47.x** (última estable de 1.20.1),
  mapeos oficiales de Mojang.
- IntelliJ IDEA: abrir `Minecraft/Mods/senjucore` como proyecto Gradle; ejecutar
  `genIntellijRuns` para tener `runClient` y `runServer`.
- Pruebas: JUnit 5 para la lógica pura (`gradlew test`). Pruebas manuales con `runServer`
  + `runClient` usando un límite corto (ej. `horasDiarias = 0.05` → 3 min).

## 7. Criterios de “terminado”

1. El panel aparece con los 5 datos, se oculta con `H`, recorre las 6 posiciones con `J`
   (incluidas medio‑izq y medio‑der) y la posición se recuerda al reiniciar el juego.
2. En servidor, el tiempo restante baja; avisos a 10 y 1 min; al llegar a 0 te expulsa.
3. Reentrar el mismo día te expulsa con el mensaje; tras la medianoche del servidor puedes
   entrar con el tiempo completo.
4. Reiniciar el servidor conserva el tiempo usado del día.
5. Los comandos `/tiempo` funcionan para operadores.
6. En individual no hay límite y el panel muestra el tiempo jugado hoy y el nombre del mundo.
7. `gradlew build` y `gradlew test` pasan.

## 8. Lo que queda a criterio del desarrollador

Colores y espaciado exactos del panel, nombres internos de clases, frecuencia de guardado
de datos, textos exactos de los mensajes (en español).

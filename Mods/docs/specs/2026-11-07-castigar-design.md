# Diseño: Castigar — Senju Core, Forge 1.20.1

Fecha: 2026-11-07
Estado: borrador para revisión

## 1. Intención

Fase 3 del Panel de Staff: aplicar castigos a jugadores desde el menú, con motivos
predefinidos, duraciones, avisos, historial y forma de deshacerlos.

## 2. Decisiones del usuario

- Castigos: **silenciar, expulsar, banear (temporal o permanente), congelar, cárcel y quitar
  horas del límite diario**.
- Flujo aceptado: Castigar → selector de jugadores → elegir castigo → elegir duración → aplicar;
  aviso al jugador y al staff; historial; comando para perdonar y para ver historial.
- **Motivos predefinidos** (lista en la configuración, se eligen con botones).

## 3. Comportamiento

### 3.1 Flujo en el panel
1. **Castigar** (antes "Próximamente") → selector de jugadores (el mismo de Revisar inventario).
2. **Elegir castigo** (ventana de 3 filas con la cabeza del jugador y 6 botones):
   Silenciar · Expulsar · Banear · Congelar · Cárcel · Quitar horas. Cada botón muestra en su
   descripción si el jugador ya tiene ese castigo activo y hasta cuándo.
3. **Elegir duración** (salvo Expulsar):
   - Silenciar / Banear / Congelar / Cárcel: 10 min · 1 h · 1 día · 7 días · Permanente.
   - Quitar horas: 30 min · 1 h · 2 h · Todo el día.
4. **Elegir motivo** de la lista configurada.
5. **Confirmar**: botón verde "Aplicar" / rojo "Cancelar" con el resumen (jugador, castigo,
   duración, motivo).

### 3.2 Efecto de cada castigo
- **Silenciar:** no puede enviar mensajes de chat ni usar `/msg`, `/tell`, `/w`, `/me`, `/say`,
  `/teammsg`; recibe "Estás silenciado hasta <fecha> (<motivo>)".
- **Expulsar:** se le desconecta con "Expulsado: <motivo>".
- **Banear:** se le desconecta y no puede volver a entrar hasta que venza (o nunca, si es
  permanente). Pantalla: "Estás baneado hasta <fecha>. Motivo: <motivo>".
- **Congelar:** no puede moverse, saltar, romper ni colocar bloques, atacar, usar ítems ni
  abrir contenedores; sí puede hablar y mirar alrededor. Mensaje en pantalla "Estás congelado".
- **Cárcel:** se le teletransporta a la cárcel; no puede salir de un radio de 8 bloques
  (vuelve al centro), ni usar `/spawn`, `/home`, `/tp`, etc. (cualquier comando salvo
  `/msg`… *(ver §5)*), ni romper/colocar bloques. Al terminar, vuelve a donde estaba antes.
  La cárcel se define con `/staff carcel fijar` en la posición actual del staff.
- **Quitar horas:** se resta tiempo del día actual del límite diario (si se queda en 0, se le
  expulsa como cuando se acaba el tiempo).
- Todos los castigos con duración siguen corriendo aunque el jugador esté desconectado
  (tiempo real), y sobreviven a reinicios del servidor.
- El staff **no puede castigar a otro staff** *(mi decisión)*.

### 3.3 Avisos e historial
- Al jugador: mensaje con el castigo, duración y motivo.
- Al staff conectado: "<staff> castigó a <jugador>: <castigo> (<duración>) — <motivo>".
- Historial persistente por jugador (incluye perdones), también anotado en
  `logs/senjucore-castigos.log`.

### 3.4 Comandos (solo staff)
- `/staff perdonar <jugador> [castigo]` — quita castigos activos (todos o uno). Funciona aunque
  el jugador esté desconectado (por nombre).
- `/staff historial <jugador>` — muestra los últimos 10 castigos.
- `/staff carcel fijar` — fija la cárcel en tu posición.
- *(mi decisión)* `/staff banear <jugador> <duración> <motivo>` para banear a alguien
  **desconectado** (el panel solo lista conectados). Duraciones: `10m`, `1h`, `1d`, `7d`, `perm`.

### 3.5 Configuración
- `motivos` en `senjucore-common.toml`, por defecto:
  `["Lenguaje ofensivo", "Spam", "Hacks o trampas", "Grief", "Acoso", "Otro"]`.

## 4. Objetivos y anti‑objetivos

**Objetivos:** castigar en pocos clics, sin errores de tipeo; que el castigo se cumpla aunque el
jugador se desconecte o el servidor se reinicie; que siempre quede registro.

**Anti‑objetivos:** que un castigado evada el castigo (reconectando, con comandos, cambiando de
dimensión, muriendo); castigar al jugador equivocado por un clic accidental (por eso la
confirmación); que un no staff pueda castigar o perdonar.

## 5. No‑objetivos (por ahora)

- Motivos escritos a mano.
- Ban por IP.
- Apelaciones, castigos automáticos o escalado (3 avisos = ban).
- Lista blanca configurable de comandos permitidos en la cárcel (por ahora: ninguno salvo chat).

## 6. Criterios de terminado

1. Desde el panel se aplica cada castigo con duración, motivo y confirmación.
2. Cada castigo tiene el efecto descrito y no se evade reconectando, cambiando de dimensión ni muriendo.
3. Los castigos vencen solos a su hora, incluso con el jugador desconectado o tras reiniciar.
4. `/staff perdonar`, `/staff historial`, `/staff carcel fijar` y `/staff banear` funcionan.
5. Avisos al jugador y al staff; historial y `logs/senjucore-castigos.log`.
6. `gradlew build test` pasa; la lógica pura (duraciones, vencimientos, historial) tiene pruebas.

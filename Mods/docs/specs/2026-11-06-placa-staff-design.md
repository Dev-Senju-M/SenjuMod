# Diseño: Placa de Staff (ítem) — Senju Core, Forge 1.20.1

Fecha: 2026-11-06
Estado: borrador para revisión

## 1. Intención

Un ítem exclusivo del staff que será la puerta de entrada a sus herramientas (castigar,
Vanish, ver inventarios y detectar objetos prohibidos). **Esta fase solo crea el ítem**: su
apariencia, quién puede tenerlo, cómo se obtiene y las reglas que impiden perderlo o
pasarlo. Las funciones del menú se diseñan después.

## 2. Decisiones del usuario

- Diseño visual: **Placa de staff** (escudo morado con borde dorado e insignia), 16×16.
- Nombre **"Placa de Staff"** en dorado, con **brillo de encantado**.
- Descripción en gris: "Herramienta exclusiva del staff" y "Clic derecho para abrir el menú".
- **No se puede tirar** del inventario.
- No se puede guardar en cofres/contenedores ni pasar a otro jugador.
- Al morir **se conserva**.
- Si un jugador que **no es staff** la tiene, **desaparece**.
- Staff = **operadores y** una **lista de nombres** en la configuración (las dos formas).
- Solo se obtiene con comando o desde creativo; no se fabrica.

## 3. Comportamiento

### 3.1 Ítem
- Id `senjucore:placa_staff`, máximo 1 por stack, rareza épica, resistente al fuego.
- Siempre con brillo (`isFoil = true`), nombre dorado, dos líneas de descripción gris.
- Textura: el boceto elegido, pulido (más contraste en la insignia y el borde dorado).
- **Clic derecho (por ahora):** mensaje en la barra de acción "Menú del staff: próximamente".
  Si quien lo usa no es staff, no pasa nada (y la placa se le quita, ver 3.3).

### 3.2 Quién es staff
- Config común nueva en `senjucore-common.toml`:
  - `staffIncluyeOperadores = true` — operadores con nivel ≥ 2 cuentan como staff.
  - `staff = []` — lista de nombres de jugador (sin distinguir mayúsculas/minúsculas).
- Es staff si (`staffIncluyeOperadores` y es operador nivel ≥ 2) **o** su nombre está en `staff`.

### 3.3 Reglas de la placa (todas en el servidor)
- **No se puede tirar:** ni con Q, ni arrastrándola fuera del inventario. Se queda en el inventario.
- **No sale del jugador:** si queda en un espacio de un contenedor abierto (cofre, barril,
  shulker, tolva, etc.), vuelve al inventario del jugador al instante. Tampoco se puede poner
  en marcos ni soportes de armadura.
- **Al morir:** no cae al suelo; el jugador la tiene al reaparecer.
- **Si no eres staff:** se revisa cada segundo; cualquier placa en tu inventario (incluida la
  mano secundaria y el cursor) se elimina, con aviso "No tienes permiso para usar la Placa de Staff".

### 3.4 Cómo se obtiene
- Comando `/staff placa` → te da una placa (si ya tienes una, no da otra).
- `/staff placa <jugador>` → se la da a otro jugador del staff (si no es staff, error).
- El comando solo lo pueden usar jugadores staff (y la consola).
- Aparece en la pestaña de creativo **"Ítems de operador"** (solo visible para operadores con esa opción activada). *(mi decisión)*

## 4. Objetivos y anti‑objetivos

**Objetivos:** que la placa nunca termine en manos de un jugador normal; que el staff no la
pierda; código listo para colgar el menú de funciones.

**Anti‑objetivos:** que un jugador normal pueda conseguir, usar o conservar la placa por
cualquier vía (cofres, morir, que un staff la tire, tolvas); que el staff pierda la placa
por un error.

## 5. Arquitectura

```
com.senju.core.staff/
  ReglaStaff.java      // LÓGICA PURA: esStaff(boolean esOp, String nombre, List<String> lista, boolean incluyeOps)
  Staff.java           // puente con Minecraft: esStaff(ServerPlayer), esStaff(CommandSourceStack)
  PlacaStaffItem.java  // el ítem: foil, nombre dorado, tooltip, onDroppedByPlayer=false, use()
  ItemsMod.java        // DeferredRegister<Item>, registro de la placa y de la pestaña de creativo
  ProteccionPlaca.java // eventos: muerte/reaparición, contenedores, marcos/soportes, quitar a no-staff
  ComandoStaff.java    // /staff placa [jugador]
assets/senjucore/
  textures/item/placa_staff.png, models/item/placa_staff.json, lang (es_es, en_us)
```

Configuración: `CommonConfig` gana `STAFF_INCLUYE_OPERADORES` y `STAFF`.

## 6. Criterios de terminado

1. `/staff placa` da la placa con su nombre dorado, brillo, descripción y textura.
2. No se puede tirar (Q ni arrastre), ni dejar en cofres/tolvas/shulkers, marcos o soportes.
3. Al morir se conserva.
4. Un jugador no staff que la reciba (p. ej. `/give`) la pierde en ≤ 1 s con aviso.
5. Quitar a alguien de `staff` o de operadores hace que pierda la placa.
6. `ReglaStaff` con pruebas automáticas; `gradlew build test` pasa.

## 7. A criterio del desarrollador

Colores exactos del pixel art, textos exactos de los mensajes, el mecanismo concreto para
devolver la placa desde un contenedor, frecuencia exacta de revisión (≤ 1 s).

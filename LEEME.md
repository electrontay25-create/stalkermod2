# Stalker Mod 1.0.5 (Minecraft 1.20.1 · Forge 47.x)

## Novedad de la 1.0.5
- Solo cambia el sonido: ahora el Stalker usa tu audio (`spy_stalker_green_screen`, 8.4 s, en bucle).
  Suena mientras el Stalker te espía y se corta cuando se esconde o desaparece.
  Lo convertí a .ogg y le subí el volumen (el original sonaba muy bajo).

## Novedades de la 1.0.4 (se mantienen)
- **Sonido de terror ligado al Stalker**: suena en bucle SOLO mientras el Stalker te observa; baja mientras se esconde
  y se corta cuando desaparece. (Sustituye al chillido y a la música anteriores.)
  El archivo es `src/main/resources/assets/stalkermod/sounds/stalker_stare.ogg` (ahora lleva tu audio).
- **Evento "asomado" rehecho**: ya no es media cabeza. Usa el PNG nuevo (cabeza + brazo levantado + cuerpo, recortado
  desde tu imagen en alta resolución). Se asoma por el borde de una pared/esquina, y si lo miras ~8 s se esconde
  DESLIZÁNDOSE lentamente detrás del muro (el brazo también se mete detrás). Solo se desvanece al final, cuando ya casi está tapado.
- **Pasillos y cuevas**: este evento solo sale cuando estás bajo techo (pasillo, cueva, casa) y se asoma a 5-18 bloques.
- **Mismo control de escondite exacto** que el acecho: solo aparece si el borde recto del PNG queda tapado.
- **Eventos naturales**: Acechar cada ~7 min (70 %), Asomado cada ~6 min (50 %), Puerta cada ~10 min (60 %).
  Si la probabilidad no sale, vuelve a tirar el dado a los ~2 min (antes esperaba el intervalo completo).
- **`/stalker status`** muestra cuánto falta para cada evento automático. En `logs/latest.log` aparecen líneas
  `[Stalker] ...` que dicen cuándo le toca a cada evento y si encontró lugar.

## Comandos (necesitan trucos)
`/stalker stalk` · `/stalker watch` · `/stalker door` · `/stalker status` · `/stalker item` · `/stalker clear`

## ACTUALIZAR EN GITHUB (tu repositorio ya existe)
1. Abre tu repositorio (pestaña **Código**).
2. **Borra la carpeta `src` vieja**: entra a `src`, toca los tres puntos (⋯) arriba a la derecha → **Eliminar directorio** → Commit.
3. Inicio del repositorio → **Agregar archivo → Subir archivos**.
4. Descomprime este zip, entra a la carpeta `stalkermod`, selecciona TODO lo de adentro (Ctrl+A) y arrástralo.
   (Sube el CONTENIDO, no el .zip.)
5. **Commit changes** → pestaña **Comportamiento (Actions)** → abre la ejecución MÁS RECIENTE (la de arriba) → ✔ verde.
6. Baja a **Artifacts** → descarga `stalkermod-jar` → dentro está `stalkermod-1.0.4.jar`.
7. En tu carpeta `mods` BORRA el .jar viejo y pon el nuevo.

## Cambiar el sonido por tu audio
Hay que convertirlo a **.ogg (Vorbis, mono)** y reemplazar `stalker_stare.ogg` (mismo nombre) en
`src/main/resources/assets/stalkermod/sounds/` del repositorio; GitHub recompila solo.

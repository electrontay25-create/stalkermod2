# Stalker Mod 1.2.0 (Minecraft 1.20.1 · Forge 47.x)

## Novedad de la 1.2.0: ANDERDINGUS (el unico que MATA)
Lo anterior sigue igual. Evento nuevo:
1. Aparece **dormido** (la pose de pie) a 14-24 bloques, en terreno abierto, un poco hacia un lado de donde miras. No suena nada.
2. Si lo **miras 3 segundos** (o te acercas a menos de 4 bloques) se **despierta**: se reproduce la animacion del video
   (fundidos entre poses hasta la forma roja furiosa) y **desde ahi empieza la cancion**.
3. Despues te **persigue**. Su velocidad sigue el **ritmo de la cancion** (analizada del audio): empieza lento (~3.2 bloques/s)
   y va cada vez mas rapido hasta ~8.5, con un pequeno impulso en cada golpe de ritmo. (Correr = 5.6 bloques/s.)
4. Si te alcanza, **te mata de un golpe** (tambien en creativo y aunque tengas totem) y desaparece.
   Si termina la cancion (56 s) sin alcanzarte, desaparece. La musica se corta cuando el desaparece.
- Frecuencia (config `anderdingus`): cada ~20 min con 40 % de probabilidad. Velocidades, segundos de mirada y volumen tambien.
- Comando de prueba: `/stalker anderdingus` (OJO: si te alcanza, mueres). Para quitarlo: `/stalker clear`.
- La animacion salio de tu video: recortada sin fondo, sin marcas de TikTok y **sin los subtitulos** ("STOP LOOKING AT... TIME!!!").

## Novedad de la 1.1.0 (se mantiene): el TORMENTOR
Evento nuevo (lo anterior sigue igual): una criatura golpea tu ventana cada vez mas fuerte hasta romperla.
- **Animacion**: los 9 segundos del video (108 cuadros, 12 por segundo), recortados sin fondo blanco y sin la marca de TikTok
  (se conservan las grietas del vidrio). Se ve como un cartel del tamano de ~2.7 bloques pegado a la ventana, por fuera.
- **Audio**: el audio del video (golpes, rugidos y vidrio roto), sincronizado: la ventana se rompe a los 9.0 s,
  que es cuando se rompe el vidrio en el video.
- **Reglas**: tienes que estar DENTRO de una casa (con techo) y cerca de una ventana (2-7 bloques) con vista a ella.
  Da igual el material: vidrio, paneles de vidrio, VALLAS o BARROTES DE HIERRO. El Tormentor los destruye igual
  (rompe tambien las ventanas pegadas, hasta 3x3).
- **Frecuencia** (config `tormentor`): cada ~12 min con 50 % de probabilidad. Si no hay ventana valida, reintenta cada 2 s hasta 3 min.
- No hace dano ni da efectos; solo rompe la ventana y se va.
- Comando de prueba: `/stalker tormentor` (dentro de tu casa, cerca de una ventana).

## Novedad de la 1.0.5 (se mantiene)
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
`/stalker stalk` · `/stalker watch` · `/stalker door` · `/stalker tormentor` · `/stalker anderdingus` · `/stalker status` · `/stalker item` · `/stalker clear`

## ACTUALIZAR EN GITHUB (tu repositorio ya existe)
1. Abre tu repositorio (pestaña **Código**).
2. **Borra la carpeta `src` vieja**: entra a `src`, toca los tres puntos (⋯) arriba a la derecha → **Eliminar directorio** → Commit.
3. Inicio del repositorio → **Agregar archivo → Subir archivos**.
4. Descomprime este zip, entra a la carpeta `stalkermod`, selecciona TODO lo de adentro (Ctrl+A) y arrástralo.
   (Sube el CONTENIDO, no el .zip.)
5. **Commit changes** → pestaña **Comportamiento (Actions)** → abre la ejecución MÁS RECIENTE (la de arriba) → ✔ verde.
6. Baja a **Artifacts** → descarga `stalkermod-jar` → dentro está `stalkermod-1.2.0.jar`.
7. En tu carpeta `mods` BORRA el .jar viejo y pon el nuevo.

## Cambiar el sonido por tu audio
Hay que convertirlo a **.ogg (Vorbis, mono)** y reemplazar `stalker_stare.ogg` (mismo nombre) en
`src/main/resources/assets/stalkermod/sounds/` del repositorio; GitHub recompila solo.

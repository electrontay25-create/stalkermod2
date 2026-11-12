# Stalker Mod 1.0.3 (Minecraft 1.20.1 · Forge 47.x)

## Novedades de la 1.0.3
- **Sonidos de puerta originales de Minecraft** (toques, golpes y puerta rota). Se acabó el sonido feo.
- **Chillido de presentimiento + música de terror nueva** cuando algo te empieza a observar.
- **Aparición exacta**: el Stalker solo sale si el borde recto del PNG queda tapado por la cobertura
  (se comprueba con rayos desde tus ojos). Forma A (figura completa) necesita cobertura de 4+ bloques de alto,
  forma B (cabeza + brazo) de 2+. Nunca debería verse "recortado" en el aire.
- **Probabilidades**: Acechar 70 %, Media cabeza 50 %, Puerta 60 % (cuando le toca). Si dice que sí pero no hay
  dónde esconderse, reintenta cada 2 s hasta 3 minutos.
- **Nuevo evento "media cabeza"**: oyes el chillido, miras y ves la mitad de la cabeza asomada detrás de un muro,
  esquina, tronco o borde de cueva (por el costado o por arriba). Si la miras ~8 s desaparece lentamente.
- **Puerta más flexible**: de día o de noche, no exige techo cerrado (basta estar adentro, cerca de la puerta),
  busca ventana hasta 4 bloques y, si no hay ventana, igual ocurre solo con sonidos (golpes y puerta rota).
- **Encadenado**: después del evento de la puerta hay 20 % de que venga otro evento enseguida.

## Comandos de prueba (necesitan trucos)
`/stalker stalk` · `/stalker watch` · `/stalker door` · `/stalker item` · `/stalker clear`

## ACTUALIZAR EN GITHUB (tu repositorio ya existe)
1. Abre tu repositorio (pestaña **Código**).
2. **Borra la carpeta `src` vieja**: entra a `src`, toca los tres puntos (⋯) arriba a la derecha → **Eliminar directorio** → Commit.
3. Inicio del repositorio → **Agregar archivo → Subir archivos**.
4. Descomprime este zip, entra a la carpeta `stalkermod`, selecciona TODO lo de adentro (Ctrl+A) y arrástralo.
   (Sube el CONTENIDO, no el .zip. `.github` no hace falta: ya está.)
5. **Commit changes** → pestaña **Comportamiento (Actions)** → abre la ejecución MÁS RECIENTE (la de arriba) → ✔ verde (3-5 min).
6. Baja a **Artifacts** → descarga `stalkermod-jar` → dentro está `stalkermod-1.0.3.jar`.
7. En tu carpeta `mods` BORRA el .jar viejo y pon el nuevo.

## Configuración
`config/stalkermod-common.toml`: intervalos, probabilidades, distancias, segundos de mirada, solo de noche, etc.
Intervalo, volumen y segundos del acecho también se cambian con el Reloj del Stalker.

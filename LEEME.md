# Stalker Mod 1.0.2 (Minecraft 1.20.1 · Forge 47.x)

## Qué cambió en la 1.0.2
- **Ya no es 3D**: el Stalker es un PNG recortado (sin fondo blanco) que siempre mira hacia ti.
- **Dos formas de asomarse**: (A) figura completa pegada al borde del árbol/pared, (B) cabeza ladeada con un brazo
  levantado que se agarra de la cobertura. Se elige al azar.
- **Aparece detrás de cualquier cosa**: troncos, hojas, piedra, paredes y casas, cuevas. Ya no solo en cuevas.
- **Aparición automática** cada 7 minutos (±20 %): primero se oye un ruido de hojas, y cuando miras, está espiándote.
- **Se esconde de verdad**: si lo miras ~4 s (o te acercas), se mete detrás de la cobertura y recién ahí desaparece.
- **Reloj del Stalker**: mantén clic derecho ~1.5 s y se abre la ventana de ajustes
  (minutos entre apariciones, volumen de la música, segundos que lo miras antes de que se esconda).
  Lo recibes solo la primera vez que entras al mundo. Receta: reloj + ojo de ender. O `/stalker item`.
- El evento de la puerta sigue igual (ahora con sprites 2D).

## Comandos de prueba (necesitan trucos)
`/stalker stalk` · `/stalker door` · `/stalker item` · `/stalker clear`

## ACTUALIZAR EN GITHUB (tu repositorio ya existe)
1. Abre tu repositorio en GitHub (pestaña **Código**).
2. **Borra la carpeta `src` vieja** (importante, si no se mezclan archivos viejos y falla):
   entra a `src`, arriba a la derecha toca los tres puntos (⋯) → **Eliminar directorio** → confirmar con **Commit**.
3. Vuelve al inicio del repositorio → **Agregar archivo → Subir archivos**.
4. Descomprime este zip, entra a la carpeta `stalkermod`, selecciona TODO lo de adentro (Ctrl+A) y arrástralo.
   (`.github` no hace falta: ya la tienes en el repositorio.)
5. **Commit changes** y ve a la pestaña **Comportamiento (Actions)**. Espera el ✔ verde (unos 3-5 min).
6. Entra a esa ejecución → **Artifacts** → descarga `stalkermod-jar` → dentro está `stalkermod-1.0.2.jar`.
7. En tu carpeta `mods` **borra el .jar viejo** y pon el nuevo.

## Configuración
`config/stalkermod-common.toml` (también se cambia con el Reloj del Stalker).

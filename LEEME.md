# Stalker Mod (Minecraft 1.20.1 · Forge 47.x)

Entidad de terror "Stalker". Dos eventos:
1. **Acechar**: aparece detrás de un árbol/pared/columna cuando NO lo miras, suena música de terror;
   si te acercas o lo miras mucho, se desvanece rápido pero no instantáneo.
2. **Puerta**: tocan 3+3 veces, silencio 5-10 s, su cara aparece en la ventana junto a la puerta,
   un brazo intenta abrirla, golpes cada vez más fuertes, rompe la puerta y te da Ceguera III por 10 s.
   No ataca.

## CÓMO OBTENER EL .JAR (sin instalar nada) — con GitHub

1. Crea cuenta en https://github.com y un repositorio nuevo (ej. `stalkermod`), público o privado.
2. Sube TODO el contenido de esta carpeta (incluida la carpeta oculta `.github`).
   - "Add file → Upload files" y arrastra todo.
   - Si la carpeta `.github` no se sube: "Add file → Create new file", escribe como nombre
     `.github/workflows/build.yml`, pega el contenido de ese archivo y guarda.
3. Ve a la pestaña **Actions**. Se ejecuta "Compilar mod (.jar)" (tarda ~5-10 min la primera vez).
4. Cuando salga con ✔ verde, entra a esa ejecución y abajo, en **Artifacts**, descarga `stalkermod-jar`.
   Dentro está `stalkermod-1.0.0.jar`.
5. Copia el .jar a la carpeta `mods` de tu Minecraft 1.20.1 con Forge 47.x.

### Alternativa local
Instala JDK 17 y Gradle 8.1.1, abre una terminal en esta carpeta y ejecuta `gradle build`.
El .jar queda en `build/libs/`.

## Si falla la compilación
Copia el error rojo del log de Actions (busca líneas con `error:`) y pásamelo: el proyecto no pudo
compilarse en el entorno donde lo generé, así que puede haber algún error de nombres de la API.

## Probar rápido (necesitas cheats / OP)
- `/stalker stalk`  → fuerza el evento de acecho (ve cerca de árboles o paredes).
- `/stalker door`   → fuerza el evento de puerta (puerta de madera cerrada con un vidrio/panel a 1-3 bloques al lado).
- `/stalker clear`  → elimina Stalkers y cancela el evento.
- Huevo generador en el creativo (pestaña Spawn Eggs); sin cobertura se verá "suelto", es solo para pruebas.

## Configuración
`config/stalkermod-common.toml` (se crea al abrir el mundo): distancias, cooldowns, probabilidades,
solo de noche, duración y nivel de ceguera, activar/desactivar cada evento.

## Sonidos
Placeholders generados por mí en `src/main/resources/assets/stalkermod/sounds/`.
Reemplázalos por tus .ogg (mono, mismo nombre) y recompila:
`stalker_ambient.ogg`, `stalker_knock.ogg`, `stalker_heavy_knock.ogg`, `stalker_door_break.ogg`.

## Textura / modelo
- Textura: `assets/stalkermod/textures/entity/stalker.png` (512x512). La cara salió de tu captura del
  mod; el resto es negro. Puedes pintarla en cualquier editor sin tocar el código.
- Modelo y animaciones están hechos por código (`client/StalkerModel.java`), no con GeckoLib.

## Limitaciones de esta versión de prueba
- La puerta no "tiembla" visualmente (suena, hay astillas y el brazo golpea).
- Sin sacudida de cámara.
- Los eventos aleatorios solo ocurren en el Overworld.

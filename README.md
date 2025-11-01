# Block ESP (NeoForge)

Cliente: Minecraft 1.21.8 • Loader: NeoForge • Plataforma: Cliente

## Resumen

- Resalta bloques e ítems con cajas y relleno opcional.
- “Ver a través” global: líneas y relleno sin pruebas de profundidad.
- Pestañas en la GUI: Bloques / Items, búsqueda persistente, navegación Prev/Next.
- Slider de radio (8–128), color ARGB y opacidad por bloque/ítem.
- Auto-actualización activa: reescaneo cada ~1s, al cargar chunks y al aparecer ítems.
- Configuración en JSON y a través de la GUI.

## Instalación

- Descarga el JAR desde Modrinth o GitHub Releases.
- Copia el archivo `block-esp-1.0.0.jar` en la carpeta `mods` de tu perfil de Minecraft con NeoForge.
- Probado en: `Minecraft 1.21.8` + `NeoForge 9.x` (cliente).
- Dependencias: ninguna.

## Uso

- Asigna las teclas en `Opciones → Controles → Asignación de teclas (Block ESP)`:
  - Activar/Desactivar ESP.
  - Recargar estilos.
  - Abrir GUI de configuración.
- En la GUI:
  - Pestañas Bloques/Items para elegir el tipo.
  - Busca por ID (`minecraft:diamond_ore`, `minecraft:iron_ingot`, etc.).
  - Alterna activación, ajusta color (`#AARRGGBB`) y opacidad.
  - Cambia el `Radio` global y el botón `Ver a través` (ON/OFF).
  - Guarda para persistir y reaplicar.

## Configuración (JSON)

- Ruta del archivo: `.minecraft/config/blockesp.json`.
- Si no existe, se usa `src/main/resources/blockesp.json` como base y se genera.

Ejemplo actualizado:

```json
{
  "radius": 48,
  "seeThrough": true,
  "blocks": {
    "minecraft:diamond_ore": { "enabled": true, "color": "#FF00FF00", "opacity": 1.0 },
    "minecraft:ancient_debris": { "enabled": true, "color": "#FFFF8800", "opacity": 0.9 }
  },
  "items": {
    "minecraft:iron_ingot":   { "enabled": true,  "color": "#FF00AAFF", "opacity": 0.8 },
    "minecraft:diamond":      { "enabled": false, "color": "#FF33CCFF", "opacity": 1.0 }
  }
}
```

Notas:
- `color` admite `#RRGGBB` o `#AARRGGBB`; si usas 6 dígitos, se aplica alfa `FF`.
- `opacity` afecta líneas; el relleno usa `opacity * 0.15` para no cubrir demasiado.

## Características técnicas

- Render: cajas por bloque (`0..1` en coordenadas locales) y por `ItemEntity` (su bounding box).
- “Ver a través”: RenderTypes personalizados que deshabilitan depth-test y depth-write.
- Eventos cliente: `ClientTick` para reescaneo periódico, `ChunkEvent.Load` y `EntityJoinLevelEvent` para actualizar al recibir datos.
- Listas concurrentes: uso de `CopyOnWriteArrayList` para evitar `ConcurrentModificationException` durante el render.

## Rendimiento

- Intervalo de reescaneo por defecto: ~1s (ajustable si lo deseas).
- Si notas coste por muchas escrituras simultáneas, usa radio menor o desactiva ítems/bloques innecesarios.
- Algunos shaders externos pueden alterar el efecto “ver a través”; desáctivalos o usa líneas sin relleno.

## Construcción desde fuente

- Requisitos: JDK 21.
- Comando: 
  
  ```
  ./gradlew build -x test
  ```
  
- Artefacto: `build/libs/block-esp-1.0.0.jar`.



## Changelog

- 1.0.0
  - Añadido resaltado de ítems (ItemEntity) con cajas y relleno opcional.
  - "Ver a través" global ON/OFF desde la GUI.
  - Slider de radio global y persistencia de búsqueda/selección.
  - Auto-actualización activa: reescaneo cada ~1s, `ChunkEvent.Load` y `EntityJoinLevelEvent`.
  - Config extendida: `seeThrough`, `items` además de `blocks` y `radius`.
  - Corrección de concurrencia en render con `CopyOnWriteArrayList`.

## Soporte

- Reporta problemas y solicitudes en `Issues` del repositorio.
- Incluye versión de Minecraft, NeoForge y logs (`latest.log`, crash report) al abrir un issue.

## Licencia

- MIT. Consulta el archivo `LICENSE` incluido en el repositorio.

## Atribuciones y aviso

- Este mod es solo de cliente. No altera gameplay del servidor.
- No incluye shaders; puede verse afectado por packs o mods de render externos.

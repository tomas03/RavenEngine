# RavenEngine

**RavenEngine** es un motor de videojuegos 2D moderno y ligero desarrollado desde cero en **Java** utilizando **LWJGL 3** y **OpenGL 3.3 Core**. Diseñado con una arquitectura modular basada en Entidades y Componentes (ECS) e integrado con un editor interactivo en tiempo real impulsado por **Dear ImGui**.

## Características Principales

### Renderizado y Gráficos (OpenGL 3.3)
- **Dynamic Batch Renderer:** Agrupamiento inteligente de quads y llamadas de dibujado (*draw calls* mínimas), con soporte para hasta 8 texturas por lote y buffers dinámicos de vértices sin impacto en el Garbage Collector.
- **Y-Sorting 2.5D Dinámico:** Algoritmo *Insertion Sort* de alto rendimiento integrado directo en los batches para calcular la profundidad vertical según la base de apoyo/pies (`ySortOffset`), permitiendo que las entidades caminen por delante o detrás de props de forma natural.
- **Text Rendering (SDF/TrueType):** Horneado reactivo de fuentes en tiempo de ejecución con *STB TrueType*, atlas dinámico de 1024x1024 y limpieza estricta de VRAM.
- **FrameBuffer Viewport:** La escena del juego corre renderizada dentro de una textura independiente dentro del editor, permitiendo aislar coordenadas de pantalla, mundo e interfaz.

### Editor & Herramientas (RavenEngine IDE)
- **Docking Completo:** Sistema multi-ventana personalizable (Scene Viewport, Jerarquía, Inspector, Terminal, Navegador de Archivos, Paleta de Tiles y Editor de Código).
- **Gizmos Interactivos en el Viewport:**
  - **Traslación (`W`):** Flechas direccionales por eje $X$/$Y$ y arrastre libre desde el pivote central.
  - **Escala (`R`):** Mangos de control cuadrados con límites mínimos de seguridad para evitar colapsos visuales.
  - Detección precisa de mouse en espacio de mundo y resaltado dinámico con `DebugDraw`.
- **Snap a Grilla & Grilla Visual:** Cuadrícula visible en tiempo real con nivel de detalle adaptativo (LOD) y ajuste magnético configurable (8px, 16px, 32px o libre manteniendo `Ctrl`). Atajo rápido: **`G`**.
- **Sistema de Deshacer / Rehacer (Undo / Redo):** Patrón *Command* desacoplado (`Ctrl+Z` / `Ctrl+Y`) para traslaciones, cambios de escala y recuperación de entidades eliminadas.
- **Inspector por Reflexión Dinámica:** Edición automática en tiempo de ejecución de tipos primitivos (`boolean`, `int`, `float`, `String`), vectores JOML (`Vector2f`, `Vector3f`, `Vector4f`) y menús desplegables para enumeraciones (`Enum`).

### Flujo de Trabajo y Datos
- **Drag & Drop:** Instanciación instantánea arrastrando archivos `.png`, `.jpg` o `.prefab` directamente desde el *Content Browser* hacia el Viewport.
- **Serialización JSON (Gson):** Guardado y carga reactiva de escenas y componentes, con snapshotting automático al alternar entre modo **Editor** y modo **Play** (reversión total al detener la simulación).
- **Project Settings Persistentes:** Ajuste global de resolución, V-Sync, gravedad Box2D y escena por defecto con exportación de configuraciones a `project_settings.json`.

---

## Stack Tecnológico

| Componente | Tecnología |
| :--- | :--- |
| **Lenguaje** | Java 17+ |
| **Gráficos & Ventana** | LWJGL 3 (GLFW, OpenGL 3.3 Core) |
| **Matemáticas** | JOML (Java OpenGL Math Library) |
| **Interfaz de Usuario (IDE)** | Dear ImGui (Java Binding `imgui-java`) |
| **Físicas** | JBox2D / Box2D 2D Physics |
| **Audio** | OpenAL / STB Vorbis |
| **Serialización** | Google Gson |

---

## Estructura del Proyecto

```
RavenEngine/
├── src/main/java/
│   ├── org.ravenEngine/
│   │   ├── audio/          # Gestión de OpenAL y AudioSource
│   │   ├── components/     # SpriteRenderer, BoxCollider2D, Inventory, etc.
│   │   ├── core/           # Ciclo principal, ProjectSettings, Window
│   │   ├── editor/         # Paneles ImGui, GizmoRenderer, Inspector
│   │   │   └── history/    # Command Pattern (Undo / Redo)
│   │   ├── io/             # KeyListener, MouseListener
│   │   ├── renderer/       # BatchRenderer, Shader, Camera, FrameBuffer
│   │   ├── scene/          # GameObject, Transform, Scene
│   │   └── util/           # AssetPool, ComponentSerializer
│   └── org.alchemistShop/  # Lógica del juego (Items, NPCs, Gameplay)
└── assets/                 # Texturas, Shaders, Sonidos, Escenas y Prefabs
```
Atajos de Teclado
W = Activar herramienta de Traslación (Gizmo flechas)
R = Activar herramienta de Escala (Gizmo mangos)
G = Alternar visibilidad de la Grilla
F = Enfocar cámara en la entidad seleccionada
Ctrl + Z = Deshacer última acción (Transform / Borrado)
Ctrl + Y / Ctrl + Shift + Z = Rehacer última acción
Ctrl + D = Duplicar entidad seleccionada
Supr / Backspace = Eliminar entidad seleccionada
Ctrl + S = Guardar escena activa
F1 = Alternar visualización de debug de colliders (Box2D)
Click Central / Click Derecho = Paneo de cámara en el Viewport
Rueda del Mouse = Zoom in / Zoom out

# Clonar el repositorio
git clone [https://github.com/tu-usuario/RavenEngine.git](https://github.com/tu-usuario/RavenEngine.git)
cd RavenEngine

# Compilar y ejecutar
mvn clean compile exec:java

📜 Licencia

Este proyecto está bajo la licencia MIT. Consultá el archivo LICENSE para más detalles.

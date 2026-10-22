# RavenEngine

**RavenEngine** is a lightweight, modern 2D game engine built from scratch in **Java 17**, utilizing **LWJGL 3** and **OpenGL 3.3 Core**. Designed with a modular Entity-Component System (ECS) architecture, it features an interactive real-time editor powered by **Dear ImGui**.

---

## Key Features

### Graphics & Rendering Pipeline (OpenGL 3.3 Core)
- **Dynamic Batch Renderer:** Efficient quad batching with minimal draw calls, dynamic vertex buffer streaming, and multi-texture binding (up to 8 textures per batch) without Garbage Collector pressure.
- **2.5D Dynamic Y-Sorting:** High-performance insertion sort integrated directly into the batching pipeline, using per-entity `ySortOffset` for natural isometric/top-down perspective occlusion.
- **Text Rendering (SDF/TrueType):** Dynamic font rasterization using *STB TrueType*, baking glyph atlases on demand into VRAM with clean resource disposal.
- **Framebuffer Viewport:** The scene runs inside an isolated offscreen framebuffer texture within the editor, separating editor UI, world space, and screen space coordinates.

### Editor & Tooling (RavenEngine IDE)
- **Full Docking Layout:** Flexible multi-window interface including Scene Viewport, Hierarchy, Inspector, Terminal/Logs, Project Explorer, Tile Palette, and Built-in Code Editor.
- **Interactive Viewport Gizmos:**
  - **Translation (`W`):** Directional axis handles (X / Y) and free planar translation from origin.
  - **Scale (`R`):** Bounding control handles with safety thresholds to prevent visual collapse.
  - **Precision Picking:** World-space mouse raycasting with dynamic collision highlighting via `DebugDraw`.
- **Snapping & Grid System:** Dynamic level-of-detail (LOD) grid overlay with configurable magnetic snapping (8px, 16px, 32px or freeform holding `Ctrl`). Toggle shortcut: `G`.
- **Undo / Redo History:** Decoupled `Command` pattern architecture supporting full rollback (`Ctrl + Z` / `Ctrl + Y`) for transforms, scaling, deletions, and hierarchy manipulations.
- **Dynamic Reflection Inspector:** Automatic inspector GUI generation for primitive types (`boolean`, `int`, `float`, `String`), JOML vectors, enums, asset references, and custom components.

### Scripting, Hot-Reloading & Architecture
- **Runtime Hot-Reload:** Compile and inject Java gameplay scripts at runtime via custom class loaders without restarting the engine state or losing scene context.
- **Dual Runtime Pipeline:** Distinct `EDITOR` and `RUNTIME` engine branches. Run and test your games inside the editor viewport or launch standalone builds directly via the `--release` flag.
- **Robust Deserialization:** Component serialization powered by Gson, built with fault-tolerant fallbacks to prevent scene crashes when encountering missing scripts or corrupt states.
- **Standalone Project Exporter:** Export complete standalone games with a single click. Packages bundled Fat JARs, project settings, assets, platform launch scripts (`.sh` / `.bat`), and window icons to any selected output directory.

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher.
- **Build Tool:** Apache Maven 3.8+.
- **Graphics:** GPU with support for OpenGL 3.3 Core Profile.

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/tomas03/RavenEngine.git](https://github.com/tomas03/RavenEngine.git)
   cd RavenEngine
   
2. Compile and package the engine:
    ```Bash

    mvn clean package

    This generates the executable Fat JAR at target/RavenEngine-IDE.jar.

    Launch the Engine IDE:
    Bash

    java -jar target/RavenEngine-IDE.jar

Standalone Distribution (jpackage)

To bundle RavenEngine as a native standalone binary without requiring a system-wide JVM:
```Bash

jpackage \
  --type app-image \
  --name "RavenEngine" \
  --input target/ \
  --main-jar RavenEngine-IDE.jar \
  --main-class org.ravenEngine.App \
  --dest dist/
```
License

This project is licensed under the MIT License - see the LICENSE file for details.

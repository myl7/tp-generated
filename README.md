# TP Generated

TP Generated is a server-side Fabric mod that lets permission-level 1 players use `/tp` and `/teleport` while preventing coordinate teleports from generating new terrain.

This document is also available in [中文](README.zh.md).

## Minecraft version

- 26.2

## Installation

Install Fabric Loader on the server, then put Fabric API and the TP Generated JAR in the server's `mods` directory. Clients do not need this mod.

## Permission behavior

The mod uses each player's existing vanilla permission level:

- Level 0 cannot use `/tp` or `/teleport`.
- Level 1 can teleport to coordinates only when the destination chunk is loaded or has completed terrain generation.
- Level 2 and above keep Minecraft's unrestricted teleport commands.

Teleporting to an entity does not need the terrain check because the target entity is already in a loaded chunk. `/execute in <dimension> run tp ...` checks the destination in the selected dimension.

The restriction applies to `/tp` and `/teleport`. Portals, ender pearls, chorus fruit, and teleports performed by other mods keep their normal behavior.

## Limitations

- The mod checks the destination chunk only. After the teleport, Minecraft may generate nearby chunks to fill the player's view distance.
- A chunk that has completed only part of terrain generation counts as ungenerated.
- Minecraft still applies its world border and build-height rules to the destination.

## Build and test

Install JDK 25, then run:

```bash
./gradlew runGameTest
./gradlew build
```

The built JAR is written to `build/libs/`.

## License

[Apache License 2.0](LICENSE)

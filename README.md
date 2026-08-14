# TP Generated

Server-side Fabric mod that lets permission-level 1 players use `/tp` and `/teleport` without letting them generate new terrain through coordinate teleports.

This document is also available in [中文](README.zh.md).

## Behavior

- Permission level 0 cannot use `/tp` or `/teleport`.
- Permission level 1 can teleport to coordinates when the destination chunk is loaded or has already reached the `minecraft:full` generation status on disk.
- Teleporting to coordinates in a never-generated or partially generated chunk requires permission level 2.
- Permission level 2 and above keep the normal unrestricted command behavior.
- Entity-target teleports are unchanged because the target entity already occupies a loaded chunk.
- Only the destination chunk is checked. The check reads its saved `Status` field without loading or generating the chunk.
- `/execute in <dimension> run tp ...` checks the destination dimension.

The mod affects the `/tp` and `/teleport` commands only. Portals, ender pearls, chorus fruit, and calls to the entity teleport API are unchanged.

## Versions

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.156.0+26.2 |
| Fabric Loom | 1.17.17 |
| Gradle | 9.5.1 |
| Java | 25 or newer |

## Installation

Install Fabric Loader, Fabric API, and the TP Generated JAR in the dedicated server's `mods` directory. Clients do not need this mod.

## Permission setup

Players need vanilla permission level 1. One option is to set the following value in `server.properties` and then use `/op`:

```properties
op-permission-level=1
```

Administrators who need a higher level can be assigned a different level in `ops.json`. Alternatively, keep the server default and edit selected entries in `ops.json` to level 1 after using `/op`.

Permission level 1 also bypasses vanilla spawn protection. Grant it only to players who should have that ability.

## Build and test

```bash
./gradlew test
./gradlew runGameTest
./gradlew build
```

The built JAR is written to `build/libs/`. Fabric GameTest verifies the command requirements and the generated-chunk gate on a test server. GitHub Actions runs the tests and uploads the built JAR.

## Known limits

- The mod checks only the chunk containing the destination. After a successful teleport, normal view-distance loading may generate neighboring chunks.
- An unloaded chunk requires one asynchronous storage read followed by a wait on the server thread. This happens only when a coordinate teleport command targets an unloaded chunk.
- A partially generated proto-chunk is treated as ungenerated until its saved status is `minecraft:full`.
- Coordinates outside the world border or build height still go through vanilla validation after the permission check.

## License

[Apache License 2.0](LICENSE)

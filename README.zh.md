# TP Generated

TP Generated 是一个纯服务端 Fabric 模组。它允许 1 级权限玩家使用 `/tp` 和 `/teleport`，同时阻止坐标传送生成新地形。

本文档也有 [English](README.md) 版本。

## Minecraft 版本

- 26.2

## 安装

在服务器安装 Fabric Loader，再把 Fabric API 和 TP Generated JAR 放入服务器的 `mods` 目录。客户端无需安装本模组。

## 权限规则

模组按照玩家已有的原版权限等级执行以下规则：

- 0 级权限不能使用 `/tp` 或 `/teleport`。
- 1 级权限只能传送到已加载或已经完成地形生成的目标区块。
- 2 级及以上权限保留原版不受限制的传送指令。

传送到实体时无需检查地形，因为目标实体已经位于加载中的区块。`/execute in <维度> run tp ...` 会检查所选维度中的目标位置。

这些限制只适用于 `/tp` 和 `/teleport`。传送门、末影珍珠、紫颂果和其他模组发起的传送会保持原有行为。

## 限制

- 模组只检查落点所在区块。传送完成后，Minecraft 可能为了填满玩家的视距而生成附近区块。
- 只完成部分地形生成的区块会被视为未生成。
- Minecraft 仍会按照世界边界和建筑高度检查目标位置。

## 构建和测试

安装 JDK 25 后运行：

```bash
./gradlew runGameTest
./gradlew build
```

构建产物位于 `build/libs/`。

## 许可证

[Apache License 2.0](LICENSE)

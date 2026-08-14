# TP Generated

TP Generated 是一个纯服务端 Fabric 模组。它允许 1 级权限玩家使用 `/tp` 和 `/teleport`，同时禁止他们通过坐标传送生成新地形。

本文档也有 [English](README.md) 版本。

## 行为

- 0 级权限不能使用 `/tp` 或 `/teleport`。
- 1 级权限可以传送到已加载的区块，或者磁盘上已达到 `minecraft:full` 生成状态的区块。
- 传送到从未生成或只完成部分生成的区块需要 2 级权限。
- 2 级及以上权限保留原版的不受限命令行为。
- 传送到实体的行为不变，因为目标实体必然位于已加载区块。
- 模组只检查落点所在区块，并直接读取存档中的 `Status` 字段，不会为了检查而加载或生成区块。
- `/execute in <维度> run tp ...` 会检查目标维度。

模组只影响 `/tp` 和 `/teleport` 命令。传送门、末影珍珠、紫颂果以及对实体传送 API 的调用不受影响。

## 版本

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 或更高 |
| Fabric API | 0.156.0+26.2 |
| Fabric Loom | 1.17.17 |
| Gradle | 9.5.1 |
| Java | 25 或更高 |

## 安装

在专用服务器安装 Fabric Loader，并把 Fabric API 和 TP Generated JAR 放进 `mods` 目录。客户端不需要安装本模组。

## 权限设置

玩家需要原版 1 级权限。一种方式是在 `server.properties` 中设置以下值，然后使用 `/op`：

```properties
op-permission-level=1
```

需要更高权限的管理员可以在 `ops.json` 中使用不同等级。也可以保留服务器默认值，在 `/op` 后把指定玩家在 `ops.json` 中的等级改为 1。

原版 1 级权限同时会绕过出生点保护，只应授予需要这一能力的玩家。

## 构建和测试

```bash
./gradlew test
./gradlew runGameTest
./gradlew build
```

构建产物位于 `build/libs/`。Fabric GameTest 会在测试服务器中验证命令权限和已生成区块限制。GitHub Actions 会运行测试并上传构建产物。

## 已知限制

- 模组只检查落点所在区块。传送成功后，原版仍可能因为视距加载而生成邻近区块。
- 目标区块未加载时，模组会发起一次异步存储读取，并在服务器线程等待结果。只有坐标传送指向未加载区块时才会进行这项读取。
- 只完成部分生成的 proto-chunk 会被视为未生成，直到存档状态达到 `minecraft:full`。
- 超出世界边界或建筑高度的坐标仍会在权限检查后由原版规则处理。

## 许可证

[Apache License 2.0](LICENSE)

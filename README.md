# tp-generated

Server-only Fabric mod，目标 Minecraft Java Edition **26.2**。修改原版 `/tp` 与 `/teleport` 的权限行为，零第三方运行时依赖（仅 Fabric API）。

## 行为

1. **权限 1 级及以上**的玩家可以使用 `/tp` 和 `/teleport`（原版要求 2 级）。
2. 传送目标为**坐标**时，检查落点所在区块是否生成过：
   - 区块当前已加载，或盘上存在且生成完整（`Status` 为 `minecraft:full`）→ 放行；
   - 从未生成过（含只生成到中间阶段的 proto-chunk）→ 要求执行者具有 **2 级**权限，否则红字报错并取消传送，且不会因此生成该区块。
3. 只检查落点所在的那一个区块，不检查邻近区块（玩家落地后视距内的未生成邻区块仍会被生成，这是接受的行为）。
4. 传送目标为**实体**时不做区块检查（目标实体必然处于已加载区块）。
5. 权限判断只用原版权限等级（26.x 的 `PermissionCheck` / `PermissionSet` 体系），等级 1 和 2 硬编码，无配置文件。
6. `/execute in <维度> run tp <坐标>` 的检查针对目标维度；`/reload` 后降权仍然生效；命令方块与数据包函数（执行权限 2 级）行为不变。

不拦截 `Entity#teleport` API、末影珍珠、紫颂果、传送门等非命令传送。

## 构建

```bash
./gradlew build
```

产物在 `build/libs/`。需要 Java 25。构建需访问 `maven.fabricmc.net` 与 Mojang 的下载服务器（本仓库由无法访问这两处的环境生成，**尚未实际编译过**，见下文「实现备注」）。

## 部署

放入服务器 `mods/`，同时安装 [Fabric API](https://modrinth.com/mod/fabric-api)。`fabric.mod.json` 中 `"environment": "server"`，仅用于专用服务器。

把普通玩家设为 1 级的两种方式：

- `server.properties` 中设 `op-permission-level=1`，然后对玩家 `/op`；管理员自己在 `ops.json` 中手动把 level 改回 4；
- 或保持默认 4，每次 `/op` 后手动编辑 `ops.json` 把该玩家的 `level` 改为 1。

注意：1 级权限附带绕过出生点保护（spawn-protection），1 级玩家可在保护区内破坏方块。

## 实现

- **降权**：`CommandRegistrationCallback` 在原版命令注册完成后（以及每次 `/reload` 重建命令树后）触发。Brigadier 对同名 literal 的重复注册只合并子节点、不覆盖 requirement，所以通过 `CommandNodeAccessor`（`@Mutable @Accessor` mixin，目标 `com.mojang.brigadier.tree.CommandNode#requirement`）直接改写根节点 `tp`、`teleport` 上已注册的谓词为 `Commands.hasPermission(Commands.LEVEL_MODERATORS)`（≥1 级）。命令树由原版在玩家加入和 op 状态变化时按 source 过滤后发送，无需手动重发。
- **区块检查**：mixin 注入 `net.minecraft.server.commands.TeleportCommand#teleportToPos`（坐标分支）的 `HEAD`。执行者 ≥2 级直接放行；否则用 `Coordinates#getPosition(source)` 解析绝对坐标，先查 `ServerLevel#hasChunkAt`（已加载即放行），未加载则通过 `chunkMap.chunkScanner().scanChunk(pos, new CollectFields(...))` 从区块存储只扫描盘上 NBT 的 `Status` 字段——该读取由 IO worker 线程执行，不触发区块加载/生成；`join()` 在主线程阻塞一次盘读，命令触发频率下可接受。`Status` 为 `minecraft:full`（兼容旧写法 `full`）才算生成过，否则抛 `SimpleCommandExceptionType` 取消命令。
- 超出世界边界或高度的坐标不特殊处理：盘上读不到就按未生成走权限检查，原版逻辑随后会给出它自己的报错。

### 实现备注（26.x API 现状）

本仓库在无法访问 Mojang/Fabric 下载服务器的环境中编写，未实际编译运行；以下名称均已对照以 26.2/26.3 为目标的开源项目（fabric-example-mod `26.2` 分支、NeoForge `26.2.x`、fabric-carpet master）逐一核对：

- 26.1 起游戏不再混淆，Yarn/intermediary 已退场，直接使用 Mojang 官方类名；Loom `1.17-SNAPSHOT` 下 `build.gradle` 不再声明 `mappings`。
- 25 年底权限系统重构：`CommandSourceStack#hasPermissionLevel(int)` 已被 `net.minecraft.server.permissions.PermissionCheck` / `PermissionSet` 取代——本 mod 使用 `Commands.LEVEL_GAMEMASTERS.check(source.permissions())` 与 `Commands.hasPermission(Commands.LEVEL_MODERATORS)`。
- 读盘扫描（`ChunkMap#chunkScanner` + `CollectFields` 只收集 `Status`）与 NeoForge `/neoforge generate` 的已生成判定实现一致。
- 若未来版本重命名 `teleportToPos` 等目标，mixin 会在服务器启动时**响亮地失败**（`defaultRequire: 1`）而非静默失效。
- 如 Loom 对 mixin 进 Brigadier（非游戏库）有障碍，可退路：删掉 `CommandNodeAccessor`，在注册回调里用反射写 `CommandNode` 的 `requirement` 字段（`Field#setAccessible(true)`）。

## 测试清单

在 26.2 的 Fabric + Carpet 专用服务器上验证：

1. 0 级玩家看不到 `/tp` 补全，执行报无权限。
2. 1 级玩家（`ops.json` 中 level 1）传送到已加载区块成功。
3. 1 级玩家传送到已生成但当前未加载的区块成功。制造方法：走到某处生成区块，回到出生点等待卸载或重启服务器，再 tp 过去。
4. 1 级玩家传送到从未生成的远处坐标被红字拒绝，且该区块未因此被生成（拒绝后用 2 级账号 `/execute if loaded <pos>` 应失败，region 文件也不应新增该区块）。
5. 2 级玩家传送到从未生成的区块成功。
6. 1 级玩家 `/tp <目标玩家>` 成功且不触发区块检查。
7. `/execute in minecraft:the_nether run tp <坐标>` 的检查针对下界维度的区块。
8. 命令方块和数据包函数（执行权限 2 级）行为不变。
9. `/reload` 后 1 级玩家仍能使用 `/tp`。
10. （可选）Carpet fake player 的权限行为与真实玩家一致。

## License

MIT

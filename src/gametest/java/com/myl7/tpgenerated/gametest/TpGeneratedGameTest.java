package com.myl7.tpgenerated.gametest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class TpGeneratedGameTest {
	private static final String GENERATED_CHUNK_FAILURE = "目标区块从未生成过，需要 2 级权限";

	@GameTest
	public void teleportCommandsRequireLevelOne(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		MinecraftServer server = helper.getLevel().getServer();
		RecordingSource levelZero = RecordingSource.create(player, PermissionSet.NO_PERMISSIONS);
		RecordingSource levelOne = RecordingSource.create(player, LevelBasedPermissionSet.MODERATOR);

		for (String name : new String[]{"tp", "teleport"}) {
			CommandNode<CommandSourceStack> node = server.getCommands().getDispatcher().getRoot().getChild(name);
			helper.assertTrue(node != null, "/" + name + " should be registered");
			helper.assertFalse(node.canUse(levelZero), "/" + name + " should reject permission level 0");
			helper.assertTrue(node.canUse(levelOne), "/" + name + " should accept permission level 1");
		}
		helper.succeed();
	}

	@GameTest
	public void levelOneMayTargetALoadedChunk(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		RecordingSource source = RecordingSource.create(player, LevelBasedPermissionSet.MODERATOR);
		BlockPos destination = helper.absolutePos(new BlockPos(2, 2, 2));
		helper.assertTrue(isChunkLoaded(helper, destination), "the test destination must be loaded");

		runTeleport(helper, source, destination);
		helper.assertFalse(source.hasFailure(GENERATED_CHUNK_FAILURE),
				"permission level 1 should pass the generated-chunk gate for a loaded chunk");
		helper.succeed();
	}

	@GameTest
	public void levelOneCannotTargetANeverGeneratedChunk(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		RecordingSource source = RecordingSource.create(player, LevelBasedPermissionSet.MODERATOR);
		BlockPos destination = new BlockPos(5_000_000, 80, 5_000_000);
		helper.assertFalse(isChunkLoaded(helper, destination), "the distant destination must start unloaded");

		runTeleport(helper, source, destination);
		helper.assertTrue(source.hasFailure(GENERATED_CHUNK_FAILURE),
				"permission level 1 should be rejected for a never-generated chunk");
		helper.assertFalse(isChunkLoaded(helper, destination),
				"the rejected command must not load the destination chunk");
		helper.succeed();
	}

	@GameTest
	public void levelTwoBypassesTheGeneratedChunkGate(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		RecordingSource source = RecordingSource.create(player, LevelBasedPermissionSet.GAMEMASTER);
		BlockPos destination = new BlockPos(10_000_000, 80, 10_000_000);
		helper.assertFalse(isChunkLoaded(helper, destination), "the distant destination must start unloaded");

		runTeleport(helper, source, destination);
		helper.assertTrue(source.failures.isEmpty(), "permission level 2 coordinate teleport should not fail");
		helper.assertFalse(source.hasFailure(GENERATED_CHUNK_FAILURE),
				"permission level 2 should bypass the generated-chunk gate");
		helper.succeed();
	}

	/** The same check the mixin makes: is the chunk column holding this position loaded? */
	private static boolean isChunkLoaded(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
	}

	private static void runTeleport(GameTestHelper helper, RecordingSource source, BlockPos destination) {
		String command = "/tp " + destination.getX() + " " + destination.getY() + " " + destination.getZ();
		helper.getLevel().getServer().getCommands().performPrefixedCommand(source, command);
	}

	private static final class RecordingSource extends CommandSourceStack {
		private final List<Component> failures = new ArrayList<>();

		private static RecordingSource create(Player player, PermissionSet permissions) {
			ServerLevel level = (ServerLevel) player.level();
			return new RecordingSource(
					CommandSource.NULL,
					player.position(),
					player.getRotationVector(),
					level,
					permissions,
					player.getName().getString(),
					player.getDisplayName(),
					level.getServer(),
					player);
		}

		private RecordingSource(
				CommandSource source,
				Vec3 position,
				Vec2 rotation,
				ServerLevel level,
				PermissionSet permissions,
				String textName,
				Component displayName,
				MinecraftServer server,
				Entity entity) {
			super(source, position, rotation, level, permissions, textName, displayName, server, entity);
		}

		@Override
		public void sendFailure(Component failure) {
			failures.add(failure);
		}

		private boolean hasFailure(String text) {
			return failures.stream().map(Component::getString).anyMatch(message -> message.contains(text));
		}
	}
}

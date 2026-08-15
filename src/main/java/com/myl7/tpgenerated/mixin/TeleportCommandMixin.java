package com.myl7.tpgenerated.mixin;

import java.util.Collection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LookAt;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {
	@Unique
	private static final SimpleCommandExceptionType TPGENERATED_UNGENERATED_CHUNK =
			new SimpleCommandExceptionType(Component.literal("目标区块从未生成过，需要 2 级权限"));

	/**
	 * Only the coordinate-target branch is gated; the entity-target branch
	 * ({@code teleportToEntity}) never needs a chunk check because the target entity is
	 * necessarily in a loaded chunk.
	 *
	 * <p>Only the single destination chunk is checked, never its neighbours. A chunk counts
	 * as generated when it is currently loaded, or when its data on disk reached the
	 * {@code minecraft:full} status; proto-chunks that only reached an intermediate
	 * generation stage count as not generated, since teleporting there would finish
	 * generating them.
	 */
	@Inject(method = "teleportToPos", at = @At("HEAD"))
	private static void tpGenerated$requireGeneratedChunk(
			CommandSourceStack source,
			Collection<? extends Entity> targets,
			ServerLevel level,
			Coordinates position,
			Coordinates rotation,
			LookAt lookAt,
			CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
		if (Commands.LEVEL_GAMEMASTERS.check(source.permissions())) {
			return;
		}

		Vec3 dest = position.getPosition(source);
		BlockPos blockPos = BlockPos.containing(dest.x, dest.y, dest.z);
		ChunkPos chunkPos = new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
		// `level` is already the destination dimension, so `/execute in ... run tp` is
		// handled correctly without extra work. The check goes through the chunk source
		// rather than Level#isLoaded, which also requires the position to be inside the
		// build height. Vanilla allows teleporting outside it, and being outside says
		// nothing about whether the chunk was generated.
		if (level.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z())) {
			return;
		}

		// Not loaded: read the chunk's Status straight from chunk storage. The scan runs on
		// the IO worker thread and never triggers chunk generation; join() blocks the server
		// thread for a single disk read, which is fine at command frequency.
		CollectFields statusField = new CollectFields(new FieldSelector(StringTag.TYPE, "Status"));
		level.getChunkSource().chunkMap.chunkScanner().scanChunk(chunkPos, statusField).join();
		if (statusField.getResult() instanceof CompoundTag tag) {
			String status = tag.getStringOr("Status", "");
			if (status.equals("minecraft:full") || status.equals("full")) {
				return;
			}
		}

		throw TPGENERATED_UNGENERATED_CHUNK.create();
	}
}

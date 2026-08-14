package com.myl7.tpgenerated;

import java.util.function.Predicate;

import com.mojang.brigadier.tree.CommandNode;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import com.myl7.tpgenerated.mixin.CommandNodeAccessor;

public class TpGenerated implements ModInitializer {
	private static final String[] COMMAND_NAMES = {"tp", "teleport"};

	@Override
	public void onInitialize() {
		// The callback fires after vanilla finished registering its commands, and fires again
		// whenever /reload rebuilds the command tree, so the relaxed requirement survives
		// reloads. The command tree is (re)sent to each player by vanilla on join and on op
		// status changes, filtered per source, so no manual resend is needed.
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			Predicate<CommandSourceStack> moderators = Commands.hasPermission(Commands.LEVEL_MODERATORS);
			for (String name : COMMAND_NAMES) {
				CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(name);
				if (node != null) {
					@SuppressWarnings("unchecked")
					CommandNodeAccessor<CommandSourceStack> accessor = (CommandNodeAccessor<CommandSourceStack>) node;
					accessor.tpGenerated$setRequirement(moderators);
				}
			}
		});
	}
}

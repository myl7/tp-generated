package com.myl7.tpgenerated.mixin;

import java.util.function.Predicate;

import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Brigadier merges duplicate literal registrations by only copying children, never the
 * requirement predicate, so the only way to relax an already-registered command is to
 * overwrite the private final {@code requirement} field on the registered node.
 */
@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor<S> {
	@Mutable
	@Accessor("requirement")
	void tpGenerated$setRequirement(Predicate<S> requirement);
}

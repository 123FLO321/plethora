package org.squiddev.plethora.integration.appliedenergistics.mixins;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.CraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.squiddev.plethora.integration.appliedenergistics.ICraftingCPUCluster;
import javax.annotation.Nullable;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public class CraftingCPUClusterMixin implements ICraftingCPUCluster {
	@Nullable
	public CraftingJob myLastJob;

	@Nullable
	public CraftingJob getLastJob() {
		return myLastJob;
	}

	@Inject(method = "submitJob", at = @At("HEAD"))
	public void submitJobHead(final IGrid g, final ICraftingJob job, final IActionSource src, final ICraftingRequester requestingMachine, final CallbackInfoReturnable<Boolean> cir) {
		if (job instanceof CraftingJob) {
			myLastJob = (CraftingJob) job;
		}
	}
}

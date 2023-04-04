package org.squiddev.plethora.integration.appliedenergistics;

import appeng.crafting.CraftingJob;

import javax.annotation.Nullable;

public interface ICraftingCPUCluster {
	@Nullable
	public CraftingJob getLastJob();
}

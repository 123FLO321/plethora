package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.crafting.CraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CraftingCPU {

	public static boolean cancelJob(ICraftingCPU cpu, @Nullable String id) {
		if (!(cpu instanceof CraftingCPUCluster)) return false;
		CraftingCPUCluster cluster = (CraftingCPUCluster) cpu;

		ICraftingLink link = cluster.getLastCraftingLink();
		if (link == null) return false;

		String currentId = link.getCraftingID();
		if (id != null && !id.equals(currentId)) {
			return false;
		}
		cluster.cancel();
		return true;
	}

	@Nullable
	public static Map<String, Object> getCurrentJob(ICraftingCPU cpu) throws LuaException {
		if (!(cpu instanceof CraftingCPUCluster)) return null;
		CraftingCPUCluster cluster = (CraftingCPUCluster) cpu;

		Map<String, Object> jobData = new HashMap<>();

		ICraftingLink link = cluster.getLastCraftingLink();
		if (link != null) {
			jobData.put("id", cluster.getLastCraftingLink().getCraftingID());
		}

		CraftingJob job = ((ICraftingCPUCluster) (Object) cluster).getLastJob();
		jobData.put("tree", CraftingJobUtils.mapJob(job, cluster));

		return jobData;
	}

}

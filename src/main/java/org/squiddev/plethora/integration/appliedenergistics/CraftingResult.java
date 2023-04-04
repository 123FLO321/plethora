package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AppEng;
import appeng.crafting.CraftingJob;
import appeng.me.helpers.MachineSource;
import com.google.common.collect.ImmutableSet;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.wrapper.FromTarget;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.integration.PlethoraIntegration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CraftingResult {
	private final Handler handler = new Handler();

	private final IGrid grid;
	private final IComputerAccess access;
	private final IActionHost source;
	private final boolean simulation;

	private boolean calculated = false;

	private ICraftingJob job = null;
	private ICraftingLink link = null;

	public CraftingResult(@Nonnull IGrid grid, @Nullable IComputerAccess access, @Nonnull IActionHost source, boolean simulation) {
		this.grid = grid;
		this.access = access;
		this.source = source;
		this.simulation = simulation;
	}

	public ICraftingCallback getCallback() {
		return handler;
	}

	public ICraftingRequester getRequester() {
		return handler;
	}

	@Nullable
	public ICraftingJob getJob() {
		return job;
	}

	@Nullable
	public ICraftingLink getLink() {
		return link;
	}

	public String getStatus() {
		if (job != null && job.isSimulation()) {
			return "missing";
		} else if (!calculated) {
			return "pending";
		} else if (link == null && simulation) {
			return "simulated";
		} else if (link == null) {
			return "unknown";
		} else if (link.isCanceled()) {
			return "canceled";
		} else if (link.isDone()) {
			return "finished";
		} else {
			return "running";
		}
	}

	/**
	 * We have this as a separate class as it implements {@link appeng.api.networking.IGridHost} and we
	 * don't want to expose the methods for that.
	 */
	private final class Handler implements ICraftingCallback, ICraftingRequester {
		@Override
		public void calculationComplete(ICraftingJob job) {
			CraftingResult.this.job = job;

			if (job.isSimulation()) {
				tryQueue(null, "missing");
			} else {
				if (!simulation) {
					ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
					link = crafting.submitJob(job, this, null, false, new MachineSource(source));
				}
				calculated = true;
			}
		}

		@Override
		public ImmutableSet<ICraftingLink> getRequestedJobs() {
			return link == null ? ImmutableSet.of() : ImmutableSet.of(link);
		}

		@Override
		public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack stack, Actionable actionable) {
			return stack;
		}

		@Override
		public void jobStateChange(ICraftingLink link) {
			tryQueue(link.getCraftingID(), getStatus());
		}

		@Override
		public IGridNode getActionableNode() {
			return source.getActionableNode();
		}
	}

	private void tryQueue(Object... args) {
		if (access != null) {
			try {
				access.queueEvent("crafting_status", args);
			} catch (RuntimeException e) {
				PlethoraIntegration.LOG.error("Cannot queue crafting callback. Probably detached from the peripheral.", e);
			}
		}
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Check if this crafting task has finished.")
	public static boolean isFinished(@FromTarget CraftingResult result) {
		ICraftingJob job = result.getJob();
		if (job != null && job.isSimulation()) return true;

		ICraftingLink link = result.getLink();
		return link != null && link.isDone();
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Check if this crafting task has been canceled.")
	public static boolean isCanceled(@FromTarget CraftingResult result) {
		ICraftingLink link = result.getLink();
		return link != null && link.isCanceled();
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Cancel this crafting task.")
	public static boolean cancel(@FromTarget CraftingResult result) {
		ICraftingLink link = result.getLink();
		if (link == null) return false;
		link.cancel();
		return true;
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Get the status for this crafting task.")
	public static String status(@FromTarget CraftingResult result) {
		return result.getStatus();
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Get the ID for this crafting task.")
	public static String getId(@FromTarget CraftingResult result) {
		ICraftingLink link = result.getLink();
		return link == null ? null : link.getCraftingID();
	}

	@PlethoraMethod(modId = AppEng.MOD_ID, doc = "-- Get the various items required for this task.")
	public static Map<String, Object> getTree(IContext<CraftingResult> context) throws LuaException {
		CraftingResult result = context.getTarget();

		CraftingJob job = (CraftingJob) result.getJob();
		if (job == null) throw new LuaException("Task is still pending");

		return CraftingJobUtils.mapJob(job, null);
	}
}

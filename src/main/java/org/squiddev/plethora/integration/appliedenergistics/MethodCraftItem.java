package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AppEng;
import appeng.me.helpers.PlayerSource;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import org.squiddev.plethora.api.method.ContextKeys;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.TypedLuaObject;
import org.squiddev.plethora.api.method.wrapper.FromContext;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.gameplay.PlethoraFakePlayer;

import javax.annotation.Nonnull;

public final class MethodCraftItem {
	private MethodCraftItem() {
	}

	@Nonnull
	@PlethoraMethod(
		modId = AppEng.MOD_ID,
		doc = "-- Craft this item, returning a reference to the crafting task."
	)
	public static TypedLuaObject<CraftingResult> craft(
		IContext<IAEItemStack> context, @FromContext IGridNode node, @FromContext IActionHost host,
		int quantity
	) {
		IGrid grid = node.getGrid();
		ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);

		IAEItemStack toCraft = context.getTarget().copy();
		toCraft.setStackSize(quantity);

		IComputerAccess computer = context.getContext(ContextKeys.COMPUTER, IComputerAccess.class);
		CraftingResult result = new CraftingResult(grid, computer, host, virtual);
		IActionSource source = new PlayerSource(new FakePlayer((WorldServer) host.getActionableNode().getWorld(), PlethoraFakePlayer.PROFILE), host);
		crafting.beginCraftingJob(node.getWorld(), grid, source, toCraft, result.getCallback());

		return context.makeChildId(result).getObject();
	}
}

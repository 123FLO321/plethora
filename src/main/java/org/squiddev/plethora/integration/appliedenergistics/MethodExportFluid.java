package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.core.AppEng;
import appeng.me.helpers.MachineSource;
import dan200.computercraft.api.lua.LuaException;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.ITransferMethod;
import org.squiddev.plethora.api.method.MarkerInterfaces;
import org.squiddev.plethora.api.method.wrapper.FromContext;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;

import static org.squiddev.plethora.integration.vanilla.method.MethodsInventoryTransfer.extractFluidHandler;

public final class MethodExportFluid {
	private MethodExportFluid() {
	}

	@PlethoraMethod(
		modId = AppEng.MOD_ID,
		doc = "-- Export this fluid from the AE network to a tank. Returns the amount transferred."
	)
	@MarkerInterfaces(ITransferMethod.class)
	public static long export(
			IContext<IAEFluidStack> baked, @FromContext IGrid grid, @FromContext IActionHost host,
			String toName, @Optional(defInt = Integer.MAX_VALUE) int limit
	) throws LuaException {
		// Find location to transfer to
		Object location = baked.getTransferLocation(toName);
		if (location == null) throw new LuaException("Target '" + toName + "' does not exist");

		// Validate our location is valid
		IFluidHandler to = extractFluidHandler(location);
		if (to == null) throw new LuaException("Target '" + toName + "' is not a tank");

		if (limit <= 0) throw new LuaException("Limit must be > 0");

		// Find the stack to extract
		IFluidStorageChannel channel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
		IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
		MachineSource source = new MachineSource(host);

		// Extract said fluid
		IAEFluidStack toExtract = baked.getTarget().copy();
		toExtract.setStackSize(Math.min(limit, toExtract.getStackSize()));
		toExtract = storageGrid.getInventory(channel).extractItems(toExtract, Actionable.MODULATE, source);
		if (toExtract == null) {
			return 0;
		}

		// Attempt to insert into the appropriate tank
		FluidStack toInsert = toExtract.getFluidStack();
		int extracted = toInsert.amount;
		int transferred = to.fill(toInsert, true);

		// If not everything could be inserted, replace back in the tank
		if (transferred < extracted) {
			FluidStack remainder = toInsert.copy();
			remainder.amount = extracted - transferred;
			storageGrid.getInventory(channel).injectItems(channel.createStack(remainder), Actionable.MODULATE, source);
		}

		return transferred;
	}
}

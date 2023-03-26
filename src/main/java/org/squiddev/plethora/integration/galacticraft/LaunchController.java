package org.squiddev.plethora.integration.galacticraft;

import dan200.computercraft.api.lua.LuaException;
import micdoodle8.mods.galacticraft.api.entity.ICargoEntity;
import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.ITransferMethod;
import org.squiddev.plethora.api.method.MarkerInterfaces;
import org.squiddev.plethora.api.method.wrapper.FromTarget;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.integration.vanilla.method.MethodsInventoryTransfer;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static org.squiddev.plethora.integration.vanilla.method.MethodsInventoryTransfer.extractHandler;

public final class LaunchController {

	private LaunchController() {
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Unloads the rocket's inventory into the specified inventory. Returns the number of items moved."
	)
	@MarkerInterfaces(ITransferMethod.class)
	public static float unload(
			IContext<TileEntityLaunchController> baked,
			String toName, @Optional(defInt = Integer.MAX_VALUE) int limit
	) throws LuaException {
		EntityAutoRocket rocket = getRocket(baked.getTarget());
		if (rocket == null) return 0;
		IItemHandler from = extractHandler(rocket);
		if (from == null) return 0;
		IItemHandler to = MethodsInventoryTransfer.getHandlerFor(baked, toName);
		int count = 0;
		for (int i = 0; i < rocket.getSizeInventory(); i++) {
			if (count >= limit) break;
			count += MethodsInventoryTransfer.moveItem(from, i, to, -1, limit - count);
		}
		return count;
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Loads the specified inventory into the rocket's inventory. Returns the number of items moved."
	)
	@MarkerInterfaces(ITransferMethod.class)
	public static float load(
			IContext<TileEntityLaunchController> baked,
			String fromName, @Optional(defInt = Integer.MAX_VALUE) int limit
	) throws LuaException {
		EntityAutoRocket rocket = getRocket(baked.getTarget());
		if (rocket == null) return 0;
		IItemHandler from = MethodsInventoryTransfer.getHandlerFor(baked, fromName);
		IItemHandler to = extractHandler(rocket);
		if (to == null) return 0;
		int count = 0;
		for (int i = 0; i < from.getSlots(); i++) {
			if (count >= limit) break;
			count += MethodsInventoryTransfer.moveItemWithCallback(from, i, limit - count, stack -> {
				ICargoEntity.EnumCargoLoadingState state = rocket.addCargo(stack, true);
				if (state == ICargoEntity.EnumCargoLoadingState.SUCCESS) {
					return ItemStack.EMPTY;
				} else {
					return stack;
				}
			});
		}
		return count;
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Gets the amount of items in the rockets inventory."
	)
	public static int getCargoCount(@FromTarget TileEntityLaunchController controller) {
		EntityAutoRocket rocket = getRocket(controller);
		if (rocket == null) return 0;
		int count = 0;
		for (int i = 0; i < rocket.getSizeInventory(); i++) {
			ItemStack stack = rocket.getStackInSlot(i);
			count += stack.getCount();
		}
		return count;
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Returns if a rocket is ready for launch and the given level is a specified the given level (defaults to 0.4)."
	)
	public static boolean isReadyForLaunch(@FromTarget TileEntityLaunchController controller, @Optional(defDoub = 0.4) float requiredLevel) {
		EntityAutoRocket rocket = getRocket(controller);
		if (rocket == null) return false;
		if (!rocket.checkLaunchValidity()) return false;
		if (!controller.validFrequency()) return false;
		float fuelLevel = (float) rocket.fuelTank.getFluidAmount() / (float) rocket.fuelTank.getCapacity();
		return fuelLevel >= requiredLevel;
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Launches the rocket."
	)
	public static void launch(@FromTarget TileEntityLaunchController controller) throws LuaException {
		EntityAutoRocket rocket = getRocket(controller);
		if (rocket == null) throw new LuaException("No rocket attached");
		Method autoLaunchMethod;
		try {
			autoLaunchMethod = EntityAutoRocket.class.getDeclaredMethod("autoLaunch");
			autoLaunchMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new LuaException("Rocket does not support auto launch");
		}
		if (!controller.validFrequency()) throw new LuaException("Launch controller is not ready for launch");
		if (!rocket.checkLaunchValidity()) throw new LuaException("Rocket is not ready for launch");
		try {
			autoLaunchMethod.invoke(rocket);
		} catch (Exception e) {
			throw new LuaException("Unable to launch rocket");
		}
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Sets the controller's launch frequency."
	)
	public static void setLaunchFrequency(@FromTarget TileEntityLaunchController controller, int frequency) {
		controller.setFrequency(frequency);
	}


	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Sets the controller's destination frequency."
	)
	public static void setDestinationFrequency(@FromTarget TileEntityLaunchController controller, int frequency) {
		controller.setDestinationFrequency(frequency);
	}

	@PlethoraMethod(
			modId = Constants.MOD_ID_PLANETS,
			doc = "-- Checks if the controller has a rocket and retrun its id."
	)
	@Nullable
	public static String hasRocket(@FromTarget TileEntityLaunchController controller) {
		EntityAutoRocket rocket = getRocket(controller);
		if (rocket == null) return null;
		return rocket.getUniqueID().toString();
	}


	@Nullable
	private static EntityAutoRocket getRocket(TileEntityLaunchController controller) {
		if (controller.attachedDock instanceof TileEntityLandingPad) {
			TileEntityLandingPad pad = ((TileEntityLandingPad) controller.attachedDock);
			IDockable rocket = pad.getDockedEntity();
			if (rocket instanceof EntityAutoRocket) {
				return (EntityAutoRocket) rocket;
			}
		}
		return null;
	}

}

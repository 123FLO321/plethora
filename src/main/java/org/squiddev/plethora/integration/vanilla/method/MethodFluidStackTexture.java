package org.squiddev.plethora.integration.vanilla.method;

import appeng.core.Api;
import appeng.core.AppEng;
import appeng.fluids.items.FluidDummyItem;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.integration.vanilla.TextureRenderer;

@SideOnly(Side.CLIENT)
public final class MethodFluidStackTexture {
	private MethodFluidStackTexture() {
	}

	@PlethoraMethod(
		modId = AppEng.MOD_ID,
		doc = "-- Renders the item stack and returns the texture as a base64 encoded string."
	)
	public static String getTexture(IContext<FluidStack> context, @Optional(defInt = 1) int scale) throws LuaException {
		if (scale < 1) throw new LuaException("Scale must be at least 1");
		if (scale > 32) throw new LuaException("Scale must be at most 32");

		try {
			// TODO: get better way to get dummy fluid stack without ae2
			ItemStack stack = Api.INSTANCE.definitions().items().dummyFluidItem().maybeStack(1).orElse(ItemStack.EMPTY);
			FluidDummyItem item = (FluidDummyItem)stack.getItem();
			item.setFluidStack(stack, context.getTarget());
			return TextureRenderer.renderedTexture(stack, scale);
		} catch (Exception e) {
			throw new LuaException("Failed to get item texture: " + e.getMessage());
		}
	}
}

package org.squiddev.plethora.integration.vanilla.method;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.integration.vanilla.TextureRenderer;

@SideOnly(Side.CLIENT)
public final class MethodItemStackTexture {
	private MethodItemStackTexture() {
	}

	@PlethoraMethod(
		doc = "-- Renders the item stack and returns the texture as a base64 encoded string."
	)
	public static String getTexture(IContext<ItemStack> context, @Optional(defInt = 1) int scale) throws LuaException {
		if (scale < 1) throw new LuaException("Scale must be at least 1");
		if (scale > 32) throw new LuaException("Scale must be at most 32");

		try {
			return TextureRenderer.renderedTexture(context.getTarget(), scale);
		} catch (Exception e) {
			throw new LuaException("Failed to get item texture: " + e.getMessage());
		}
	}
}

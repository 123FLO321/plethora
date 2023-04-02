package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.storage.data.IAEFluidStack;
import appeng.core.AppEng;
import dan200.computercraft.api.lua.LuaException;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.wrapper.Optional;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;
import org.squiddev.plethora.integration.vanilla.TextureRenderer;

@SideOnly(Side.CLIENT)
public final class MethodAEFluidStackTexture {
	private MethodAEFluidStackTexture() {
	}

	@PlethoraMethod(
		modId = AppEng.MOD_ID,
		doc = "-- Renders the ae stack and returns the texture as a base64 encoded string."
	)
	public static String getTexture(IContext<IAEFluidStack> context, @Optional(defInt = 1) int scale) throws LuaException {
		if (scale < 1) throw new LuaException("Scale must be at least 1");
		if (scale > 32) throw new LuaException("Scale must be at most 32");

		try {
			return TextureRenderer.renderedTexture(context.getTarget().asItemStackRepresentation(), scale);
		} catch (Exception e) {
			throw new LuaException("Failed to get item texture: " + e.getMessage());
		}
	}

}

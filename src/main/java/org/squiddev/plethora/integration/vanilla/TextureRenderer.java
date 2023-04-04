package org.squiddev.plethora.integration.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@SideOnly(Side.CLIENT)
public class TextureRenderer {

	public static String renderedTexture(ItemStack stack, int scale) throws Exception {
		CompletableFuture<String> futureResult = new CompletableFuture<>();

		Minecraft.getMinecraft().addScheduledTask(() -> {
			try {
				BufferedImage bufferedImage = renderStack(stack, scale);
				String base64 = encodeImage(bufferedImage);
				futureResult.complete(base64);
			} catch (Exception e) {
				futureResult.completeExceptionally(e);
			}
		});

		return futureResult.get();
	}

	private static BufferedImage renderStack(ItemStack stack, int scale) throws Exception {
		Minecraft mc = Minecraft.getMinecraft();

		RenderItem renderItem = mc.getRenderItem();

		int width = mc.displayWidth;
		int height = mc.displayHeight;

		int actualScale = (int) (getCurrentGuiScale(mc) * scale);

		GlStateManager.pushMatrix();
		GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GlStateManager.enableAlpha();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableLighting();
		GlStateManager.enableDepth();
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.translate(0.0F, 0.0F, 32.0F);
		GlStateManager.scale(actualScale, actualScale, actualScale);
		RenderHelper.enableGUIStandardItemLighting();
		renderItem.zLevel = 200.0F;
		renderItem.renderItemAndEffectIntoGUI(stack, 0, 0);

		int iconSize = (32 * scale);

		BufferedImage bufferedImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
		IntBuffer pixels = BufferUtils.createIntBuffer(width*height);
		int[] pixelsArray = new int[width*height];

		GlStateManager.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
		pixels.get(pixelsArray);
		mirror(pixelsArray, width, height);
		bufferedImage.setRGB(0, 0, iconSize, iconSize, pixelsArray, 0, width);

		RenderHelper.disableStandardItemLighting();
		renderItem.zLevel = 0.0f;
		GlStateManager.popMatrix();
		return bufferedImage;
	}

	private static double getCurrentGuiScale(Minecraft mc) throws Exception {
		// TODO: Find a better way to get the current gui scale
		if (mc.gameSettings.guiScale == 2) {
			return 1.0;
		} else {
			throw new Exception("Gui scale must be set to normal");
		}
	}

	private static void mirror(int[] par0ArrayOfInteger, int width, int height) {
		int[] aint1 = new int[width];
		int k = height / 2;

		for (int l = 0; l < k; ++l) {
			System.arraycopy(par0ArrayOfInteger, l * width, aint1, 0, width);
			System.arraycopy(par0ArrayOfInteger, (height - 1 - l) * width, par0ArrayOfInteger, l * width, width);
			System.arraycopy(aint1, 0, par0ArrayOfInteger, (height - 1 - l) * width, width);
		}
	}

	@Nullable
	private static String encodeImage(BufferedImage image) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (final IOException ioe) {
			return null;
		}
	}
}

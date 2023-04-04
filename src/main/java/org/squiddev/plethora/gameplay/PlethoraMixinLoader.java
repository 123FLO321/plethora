package org.squiddev.plethora.gameplay;

import appeng.core.AppEng;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixins;
import zone.rong.mixinbooter.MixinLoader;

@MixinLoader
public class PlethoraMixinLoader {

	public PlethoraMixinLoader() {
		if (Loader.isModLoaded(AppEng.MOD_ID)) Mixins.addConfiguration("mixins.ae2.json");
	}
}

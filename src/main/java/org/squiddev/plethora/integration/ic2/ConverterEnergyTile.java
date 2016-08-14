package org.squiddev.plethora.integration.ic2;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.squiddev.plethora.api.converter.IConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Gets the tile from a particular position
 */
@IConverter.Inject(TileEntity.class)
public class ConverterEnergyTile implements IConverter<TileEntity, IEnergyTile> {
	@Nullable
	@Override
	public IEnergyTile convert(@Nonnull TileEntity from) {
		World world = from.getWorld();
		if (world == null) return null;

		BlockPos pos = from.getPos();
		if (pos == null) return null;

		return EnergyNet.instance.getSubTile(world, pos);
	}
}

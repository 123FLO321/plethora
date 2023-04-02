package org.squiddev.plethora.integration.vanilla.method;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.TypedLuaObject;
import org.squiddev.plethora.api.method.wrapper.PlethoraMethod;

import java.util.ArrayList;
import java.util.List;

public final class MethodsCommandBlock {
	private MethodsCommandBlock() {
	}

	@PlethoraMethod(
		doc = "function():ItemStack[] -- Gets all registered items and variants."
	)
	public static List<TypedLuaObject<ItemStack>> getAllItems(IContext<TileEntityCommandBlock> context) {
		List<TypedLuaObject<ItemStack>>  items = new ArrayList();
		for (Item item : Item.REGISTRY) {
			if (item.getHasSubtypes()) {
				NonNullList<ItemStack> subItems = NonNullList.create();
				for (CreativeTabs tab : item.getCreativeTabs()) {
					item.getSubItems(tab, subItems);
				}
				for (ItemStack subItem : subItems) {
					subItem.setCount(1);
					items.add(context.makeChildId(subItem).getObject());
				}
			} else {
				ItemStack stack = new ItemStack(item, 1);
				items.add(context.makeChildId(stack).getObject());
			}
		}
		return items;
	}

	@PlethoraMethod(
		doc = "function():ItemStack[] -- Gets all registered fluids."
	)
	public static List<TypedLuaObject<FluidStack>> getAllFluids(IContext<TileEntityCommandBlock> context) {
		List<TypedLuaObject<FluidStack>> fluids = new ArrayList();
		for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
			FluidStack stack = new FluidStack(fluid, 1);
			fluids.add(context.makeChildId(stack).getObject());
		}
		return fluids;
	}

}

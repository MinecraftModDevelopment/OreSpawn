package com.mcmoddev.orespawn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mcmoddev.orespawn.util.StateUtil;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public class ReplacementsRegistry {
	private static Map<String,IBlockState> blocks = new HashMap<>();
	
	private ReplacementsRegistry() {
	}
	
	@SuppressWarnings("deprecation")
	public static List<IBlockState> getDimensionDefault(int dimension) {
		String[] names = { "minecraft:netherrack", "minecraft:stone", "minecraft:end_stone" };
		if( dimension < -1 || dimension > 1 || dimension == 0) {
			List<IBlockState> rv = new ArrayList<>();
			for( ItemStack iS : OreDictionary.getOres("stone") ) {
				rv.add( Block.getBlockFromItem(iS.getItem()).getStateFromMeta(iS.getMetadata()) );
			}
			return rv;
		}
		return Arrays.asList(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(names[dimension+1])).getDefaultState());
	}

	public static IBlockState getBlock(String name) {
		return blocks.get(name);
	}
	
	public static void addBlock(String name, String blockName, String blockState) {
		Block nb = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
		blocks.put(name, "default".equals(blockState)?nb.getDefaultState():StateUtil.deserializeState(nb, blockState));
	}

	public static Map<String,IBlockState> getBlocks() {
		return Collections.unmodifiableMap(blocks);
	}

	public static void addBlock(String name, IBlockState state) {
		blocks.put(name, state);
	}
}

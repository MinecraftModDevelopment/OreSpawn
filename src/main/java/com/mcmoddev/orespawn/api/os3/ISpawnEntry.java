package com.mcmoddev.orespawn.api.os3;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;

import com.mcmoddev.orespawn.api.IBlockList;

public interface ISpawnEntry {
	default public boolean isEnabled() {
		return false;
	}
	
	default public boolean isRetrogen() {
		return false;
	}
	
	public String getSpawnName();
	public boolean dimensionAllowed(final int dimension);
	public boolean biomeAllowed(final ResourceLocation biomeName);
	public boolean biomeAllowed(final Biome biome);
	public IFeatureEntry getFeature();
	public OreSpawnBlockMatcher getMatcher();
	public IBlockList getBlocks();

	public void generate(World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider, ChunkPos pos);
}

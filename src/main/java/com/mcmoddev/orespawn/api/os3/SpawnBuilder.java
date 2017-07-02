package com.mcmoddev.orespawn.api.os3;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.mcmoddev.orespawn.api.BiomeLocation;
import com.mcmoddev.orespawn.api.IFeature;

import net.minecraft.block.state.IBlockState;

public interface SpawnBuilder {
	FeatureBuilder FeatureBuilder( @Nonnull String featureName );
	BiomeBuilder BiomeBuilder();
	OreBuilder OreBuilder();
	SpawnBuilder create( @Nonnull BiomeLocation biomes, @Nonnull FeatureBuilder feature, 
			@Nonnull List<IBlockState> replacements, @Nonnull OreBuilder... ores );
	
	BiomeLocation getBiomes();
	ImmutableList<OreBuilder> getOres();
	ImmutableList<IBlockState> getReplacementBlocks();
	IFeature getFeatureGen();
}
package com.mcmoddev.orespawn.impl.features;

import java.util.List;
import java.util.Random;

import com.google.gson.JsonObject;
import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.api.FeatureBase;
import com.mcmoddev.orespawn.api.IFeature;
import com.mcmoddev.orespawn.data.Constants.FormatBits;
import com.mcmoddev.orespawn.util.OreList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;

public class PrecisionGenerator extends FeatureBase implements IFeature {
	
	public PrecisionGenerator(Random rand) {
		super(rand);
	}

	public PrecisionGenerator() {
		super( new Random() );
	}
	

	@Override
	public void generate(ChunkPos pos, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider,
			JsonObject parameters, OreList ores, List<IBlockState> blockReplace) {
		// First, load cached blocks for neighboring chunk ore spawns
		int chunkX = pos.x;
		int chunkZ = pos.z;

		mergeDefaults(parameters, getDefaultParameters());

		runCache(chunkX, chunkZ, world, blockReplace);
		
		// extract parameters
		int nodeCount = parameters.get(FormatBits.NODE_COUNT).getAsInt();
		int maxHeight = parameters.get(FormatBits.MAX_HEIGHT).getAsInt();
		int minHeight = parameters.get(FormatBits.MIN_HEIGHT).getAsInt();
		int nodeSize  = parameters.get(FormatBits.NODE_SIZE).getAsInt();
	
		// now to use them
		for( int c = nodeCount; c >= 0; c-- ) {
			HeightRange hr = new HeightRange(minHeight, maxHeight);
			BlockPos spot = chooseSpot(chunkX, chunkZ, hr);
			spawnAtSpot( spot, nodeSize, hr, world, blockReplace, ores, pos);
		}
	}

	private void spawnAtSpot(BlockPos spot, int nodeSize, HeightRange heightRange, World world, List<IBlockState> blockReplace,
			OreList ores, ChunkPos pos ) {
		int spawned = 0;
		int wanted = nodeSize;
		int c = 0;
		
		BlockPos act = spot;
		for( int counter = nodeSize; counter > 0 && spawned < wanted; counter -- ) {
			c = spawnOreNode( act, nodeSize, heightRange, pos, blockReplace, ores, world );
			if( c == 0 ) {
				OreSpawn.LOGGER.debug("Unable to place block at %s (chunk %s)", spot, pos);
				act = chooseSpot( Math.floorDiv(spot.getX(),16), Math.floorDiv(spot.getZ(), 16), heightRange );
			}
			spawned += c;
			counter -= c;
		}
		
	}
	
	private int spawnOreNode(BlockPos spot, int nodeSize, HeightRange heightRange, ChunkPos pos, 
			List<IBlockState> blockReplace,	OreList ores, World world ) {
		int count = nodeSize;
		int lutType = (nodeSize < 8)?offsetIndexRef_small.length:offsetIndexRef.length;
		int[] lut = (nodeSize < 8)?offsetIndexRef_small:offsetIndexRef;
		Vec3i[] offs = new Vec3i[lutType];

		System.arraycopy((nodeSize < 8)?offsets_small:offsets, 0, offs, 0, lutType);
		
		if( nodeSize < 27 ) {
			int[] scrambledLUT = new int[lutType];
			System.arraycopy(lut, 0, scrambledLUT, 0, scrambledLUT.length);
			scramble(scrambledLUT, this.random);
			
			int nc = 0;
			for( ; count > 0 && nc <= nodeSize; count-- ) {
				IBlockState oreBlock = ores.getRandomOre(this.random).getOre();
				Vec3i offset = offs[scrambledLUT[--count]];
				BlockPos p = fixMungeOffset( offset, spot, heightRange, pos);
				
				if( spawn(oreBlock,world, p, world.provider.getDimension(),true,blockReplace) ) {
					nc++;
				}
			}
			return nc;
		}

		return spawnFill( spot, ores, nodeSize, blockReplace, heightRange, pos, world );
	}

	private BlockPos fixMungeOffset(Vec3i offset, BlockPos spot, HeightRange heightRange, ChunkPos pos) {
		BlockPos p = spot.add(offset);
		//ChunkPos x1z1 = new ChunkPos(pos.x+1, pos.z+1);
		int xMax = pos.getXEnd();
		int zMax = pos.getZEnd();
		int xMin = pos.getXStart();
		int zMin = pos.getZStart();
		
		int xmod = offset.getX();
		int ymod = offset.getY();
		int zmod = offset.getZ();

		// correct the points values to not cause the Y coordinate to go outside the permissable range
		if( p.getY() < heightRange.getMin() || p.getY() > heightRange.getMax() ) {
			ymod = rescaleOffset(ymod, spot.getY(), heightRange.getMin(), heightRange.getMax() );
		}
		
		if( p.getX() < xMin || p.getX() > xMax ) {
			xmod = rescaleOffset( xmod, spot.getX(), xMin, xMax );
		}
		
		if( p.getZ() < zMin || p.getZ() > zMax) {
			zmod = rescaleOffset( zmod, spot.getZ(), zMin, zMax );
		}
		
		BlockPos rVal = spot.add(xmod, ymod, zmod);
		OreSpawn.LOGGER.debug("rescaled %s to %s", spot, rVal);
		return rVal;
	}

	private int rescaleOffset(final int offsetIn, final int centerIn, final int minimumIn, final int maximumIn) {
		int actual = centerIn + offsetIn;
		int wrapDistance = 0;
		int rangeMin = minimumIn;
		int rangeMax = maximumIn;
		int range = maximumIn - minimumIn;
		int workingPoint = 0;
		
		if( actual < rangeMin ) {
			wrapDistance = rangeMin - actual;
		} else {
			wrapDistance = actual - rangeMax;
		}
		
		if( wrapDistance < 0 ) {
			wrapDistance = ((-1)*wrapDistance)%range;
		} else {
			wrapDistance %= range;
		}
		
		if( actual < rangeMin ) {
			workingPoint = rangeMax - wrapDistance;
		} else {
			workingPoint = rangeMin + wrapDistance;
		}
		
		return workingPoint - centerIn;
	}

	private int spawnFill(BlockPos spot, OreList ores, int nodeSize, List<IBlockState> blockReplace, HeightRange heightRange,
			ChunkPos pos, World world) {
		double radius = Math.pow(nodeSize, 1.0/3.0) * (3.0 / 4.0 / Math.PI) + 2;
		int rSqr = (int)Math.ceil(radius * radius);
		if( this.random.nextBoolean() ) {
			return spawnMungeNE( world, new Object[] { spot, pos, heightRange }, new int[] { rSqr, nodeSize }, radius, blockReplace, ores );
		} else {
			return spawnMungeSW( world, new Object[] { spot, pos, heightRange }, new int[] { rSqr, nodeSize }, radius, blockReplace, ores );
		}
	}

	private int spawnMungeSW(World world, Object[] oParams, int[] iParams, double radius, List<IBlockState> blockReplace, OreList ores) {
		int quantity = iParams[1];
		int rSqr = iParams[0];
		int nc = 0;
		
		BlockPos spot = (BlockPos)oParams[0];
		ChunkPos pos = (ChunkPos)oParams[1];
		HeightRange heightRange = (HeightRange)oParams[2];
		
		for(int dy = (int)(-1 * radius); dy < radius; dy++){
			for(int dx = (int)(radius); dx >= (int)(-1 * radius); dx--){
				for(int dz = (int)(radius); dz >= (int)(-1 * radius); dz--){
					int total = dx*dx + dy*dy + dz*dz;
					if(total <= rSqr){
						BlockPos p = fixMungeOffset(new Vec3i(dx, dy, dz), spot, heightRange, pos);
						nc += doMungeSpawn(ores,world, p, blockReplace);
						quantity--;
					}
					
					if(quantity <= 0 || nc >= iParams[1]) {
						return nc;
					}
				}
			}
		}
		return nc;
	}
	
	private int doMungeSpawn(OreList ores, World world, BlockPos spot, List<IBlockState> blockReplace) {
		return spawn(ores.getRandomOre(this.random).getOre(),world,spot,world.provider.getDimension(),true,blockReplace)?1:0;
	}

	private int spawnMungeNE(World world, Object[] oParams, int[] iParams, double radius, List<IBlockState> blockReplace, OreList ores) {
		int rSqr = iParams[0];
		int quantity = iParams[1];
		int nc = 0;
		
		BlockPos spot = (BlockPos)oParams[0];
		ChunkPos pos = (ChunkPos)oParams[1];
		HeightRange heightRange = (HeightRange)oParams[2];
		
		for(int dy = (int)(-1 * radius); dy < radius; dy++){
			for(int dz = (int)(-1 * radius); dz < radius; dz++){
				for(int dx = (int)(-1 * radius); dx < radius; dx++){
					int total = dx*dx + dy*dy + dz*dz;
					if(total <= rSqr){					
						BlockPos p = fixMungeOffset(new Vec3i(dx, dy, dz), spot, heightRange, pos);
						nc += doMungeSpawn(ores,world, p, blockReplace);
						quantity--;
					}
					
					if(quantity <= 0 || nc >= iParams[1]) {
						return nc;
					}
				}
			}
		}
		return nc;
	}

	private double triangularDistribution(double a, double b, double c) {
	    double base = (c - a) / (b - a);
	    double rand = (this.random.nextDouble() * (b - a)) + a;
	    if (rand < base) {
	        return a + Math.sqrt(rand * (b - a) * (c - a));
	    } else {
	        return b - Math.sqrt((1 - rand) * (b - a) * (b - c));
	    }
	}

	private int getPoint( int lowerBound, int upperBound, int median ) {
		int t = (int)Math.round( triangularDistribution((float)lowerBound, (float)upperBound, (float)median) );
		return t - median;
	}

	private BlockPos chooseSpot(int xPosition, int zPosition, HeightRange heightRange) {
		int xRet = getPoint( 0, 15, 8 ) + (xPosition * 16);
		int zRet = getPoint( 0, 15, 8 ) + (zPosition * 16);
		int yRange = heightRange.getRange();
		int yMod = yRange/2;
		int yRet = getPoint( heightRange.getMin(), heightRange.getMax(), yMod );
		
		return new BlockPos( xRet, yRet, zRet );
	}

	@Override
	public void setRandom(Random rand) {
		this.random = rand;
	}

	@Override
	public JsonObject getDefaultParameters() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty(FormatBits.NODE_COUNT, 4);
		defaults.addProperty(FormatBits.MIN_HEIGHT, 16);
		defaults.addProperty(FormatBits.MAX_HEIGHT, 80);
		defaults.addProperty(FormatBits.NODE_SIZE, 8);
		return defaults;
	}

	private class HeightRange {
		private int min;
		private int max;
		
		public HeightRange( int min, int max ) {
			this.min = min;
			this.max = max;
		}
		
		public int getRange() {
			return this.max - this.min;
		}
		
		public int getMin() {
			return this.min;
		}
		
		public int getMax() {
			return this.max;
		}
	}
}
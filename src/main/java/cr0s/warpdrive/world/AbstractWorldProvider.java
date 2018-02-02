package cr0s.warpdrive.world;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.render.RenderBlank;
import cr0s.warpdrive.render.RenderSpaceSky;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProvider;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractWorldProvider extends WorldProvider {
	
	protected CelestialObject celestialObjectDimension = null;
	protected boolean isRemote;
	
	protected void updateCelestialObject() throws RuntimeException {
		if (getDimension() == 0) {
			throw new RuntimeException("Critical error: you can't use a WorldProvider before settings its dimension id!");
		}
		if (celestialObjectDimension == null) {
			isRemote = FMLCommonHandler.instance().getEffectiveSide().isClient();
			celestialObjectDimension = CelestialObjectManager.get(isRemote, getDimension(), 0, 0);
		}
	}
	
	@Nonnull
	@Override
	public String getSaveFolder() {
		updateCelestialObject();
		if (celestialObjectDimension == null) {
			throw new RuntimeException(String.format("Critical error: there's no celestial object defining %s dimension DIM%d, unable to proceed further",
			                                         isRemote ? "client" : "server", getDimension()));
		}
		return celestialObjectDimension.id;
	}
	
	/* @TODO MC1.10 dimension name
	@Override
	public String getDimensionName() {
		updateCelestialObject();
		if (celestialObjectDimension == null) {
			if (isRemote) {
				return String.format("DIM%d", getDimension());
			} else {
				throw new RuntimeException(String.format("Critical error: there's no celestial object defining %s dimension id %d, unable to proceed further",
				                                         "server", getDimension()));
			}
		}
		return celestialObjectDimension.id;
	}
	/**/
	
	@Override
	public boolean canCoordinateBeSpawn(int x, int z) {
		final BlockPos blockPos = worldObj.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
		return blockPos.getY() != 0;
	}
	
	/*
	@Override
	public BlockPos getEntrancePortalLocation() {
		return null;
	}
	
	@Nonnull
	@Override
	public BlockPos getRandomizedSpawnPoint() {
		BlockPos blockPos = new BlockPos(worldObj.getSpawnPoint());
		// boolean isAdventure = worldObj.getWorldInfo().getGameType() == EnumGameType.ADVENTURE;
		int spawnFuzz = 100;
		int spawnFuzzHalf = spawnFuzz / 2;
		{
			blockPos = new BlockPos(
				blockPos.getX() + worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf,
				200,
				blockPos.getZ() + worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf);
		}
		
		if (worldObj.isAirBlock(blockPos)) {
			worldObj.setBlockState(blockPos, Blocks.STONE.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 1, 1,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 1, 2,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add(-1, 1,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add(-1, 2,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 1,  1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 2,  1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 1, -1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 2, -1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 3,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 0,  0), WarpDrive.blockAir.getStateFromMeta(15), 2);
			worldObj.setBlockState(blockPos.add( 0, 1,  0), WarpDrive.blockAir.getStateFromMeta(15), 2);
		}
		
		return blockPos;
	}
	/**/
	
	// shared for getFogColor(), getStarBrightness()
	// @SideOnly(Side.CLIENT)
	protected static CelestialObject celestialObject = null;
	
	@SideOnly(Side.CLIENT)
	@Nonnull
	@Override
	public Vec3d getSkyColor(@Nonnull Entity cameraEntity, float partialTicks) {
		if (getCloudRenderer() == null) {
			setCloudRenderer(RenderBlank.getInstance());
		}
		if (getSkyRenderer() == null) {
			setSkyRenderer(RenderSpaceSky.getInstance());
		}
		
		celestialObject = cameraEntity.worldObj == null ? null : CelestialObjectManager.get(
			cameraEntity.worldObj,
			MathHelper.floor_double(cameraEntity.posX), MathHelper.floor_double(cameraEntity.posZ));
		if (celestialObject == null) {
			return new Vec3d(1.0D, 0.0D, 0.0D);
		} else {
			return new Vec3d(celestialObject.backgroundColor.red, celestialObject.backgroundColor.green, celestialObject.backgroundColor.blue);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Nonnull
	@Override
	public Vec3d getFogColor(float celestialAngle, float par2) {
		final float factor = Commons.clamp(0.0F, 1.0F, MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
		
		float red   = celestialObject == null ? 0.0F : celestialObject.colorFog.red;
		float green = celestialObject == null ? 0.0F : celestialObject.colorFog.green;
		float blue  = celestialObject == null ? 0.0F : celestialObject.colorFog.blue;
		float factorRed   = celestialObject == null ? 0.0F : celestialObject.factorFog.red;
		float factorGreen = celestialObject == null ? 0.0F : celestialObject.factorFog.green;
		float factorBlue  = celestialObject == null ? 0.0F : celestialObject.factorFog.blue;
		red   *= factor * factorRed   + (1.0F - factorRed  );
		green *= factor * factorGreen + (1.0F - factorGreen);
		blue  *= factor * factorBlue  + (1.0F - factorBlue );
		return new Vec3d(red, green, blue);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public float getStarBrightness(float partialTicks) {
		if (celestialObject == null) {
			return 0.0F;
		}
		final float starBrightnessVanilla = super.getStarBrightness(partialTicks);
		return celestialObject.baseStarBrightness + celestialObject.vanillaStarBrightness * starBrightnessVanilla;
	}
}
package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.*;

public class ForceFieldSetup extends GlobalPosition {
	private static final float FORCEFIELD_BASE_SCAN_SPEED = 500;
	private static final float FORCEFIELD_BASE_PLACE_SPEED = 100;
	private static final float FORCEFIELD_MAX_SCAN_SPEED = 500;
	private static final float FORCEFIELD_MAX_PLACE_SPEED = 100;
	
	public final int beamFrequency;
	public final byte tier;
	public final Set<TileEntityForceFieldProjector> projectors = new HashSet<>();
	private int lightCamouflage;
	private Block blockCamouflage;
	private int metadataCamouflage;
	private static final List<Integer> ALLOWED_RENDER_TYPES = Arrays.asList(0, 1, 4, 5, 6, 8, 7, 12, 13, 14, 16, 17, 20, 23, 29, 30, 31, 39);
	private final HashMap<IForceFieldUpgradeEffector, Float> upgrades = new HashMap<>(EnumForceFieldUpgrade.length);
	
	public float scanSpeed;
	public float placeSpeed;
	private float startupEnergyCost;
	public float scanEnergyCost;
	public float placeEnergyCost;
	private float entityEnergyCost;
	
	public float disintegrationLevel;
	public float temperatureLevel;
	public boolean hasStabilize;
	public boolean hasFusion;
	public boolean isInverted;
	public float thickness;
	public float maxPumpViscosity;
	
	// Projector provided properties
	public float rotationYaw;
	public float rotationPitch;
	public VectorI vTranslation = new VectorI(0, 0, 0);
	public VectorI vMin = new VectorI(-8, -8, -8);
	public VectorI vMax = new VectorI(8, 8, 8);
	public IForceFieldShape shapeProvider;
	public boolean isDoubleSided = true;
	
	public ForceFieldSetup(final int dimensionId, final int x, final int y, final int z, final byte tier, final int beamFrequency) {
		super(dimensionId, x, y, z);
		this.tier = tier;
		this.beamFrequency = beamFrequency;
		refresh();
	}
	
	public boolean isAccessGranted(EntityPlayer entityPlayer, PermissionNode permissionNode) {
		return false; // TODO
	}
	
	private static boolean isValidCamouflage(final Block block) {
		return block != null && block != Blocks.air && ALLOWED_RENDER_TYPES.contains(block.getRenderType());
	}
	
	public Block getCamouflageBlock() {
		if (isValidCamouflage(blockCamouflage)) {
			return blockCamouflage;
		}
		return null;
	}
	
	public int getCamouflageMetadata() {
		if (isValidCamouflage(blockCamouflage)) {
			return metadataCamouflage;
		}
		return 0;
	}
	
	public int getCamouflageLight() {
		if (isValidCamouflage(blockCamouflage)) {
			return lightCamouflage;
		}
		return 0;
	}
	
	private float getScaledUpgrade(final IForceFieldUpgradeEffector effector) {
		Float scaledValue = upgrades.get(effector);
		return scaledValue == null ? 0.0F : scaledValue;
	}
	
	private void refresh() {
		Set<TileEntity> tileEntities = ForceFieldRegistry.getTileEntities(beamFrequency);
		HashMap<IForceFieldUpgradeEffector, Float> upgradeValues = new HashMap<>(EnumForceFieldUpgrade.length);
		
		for (TileEntity tileEntity : tileEntities) {
			// only consider same dimension
			if (tileEntity.getWorldObj().provider.dimensionId != dimensionId) {
				continue;
			}
			// projectors
			if (tileEntity instanceof TileEntityForceFieldProjector) {
				TileEntityForceFieldProjector projector = (TileEntityForceFieldProjector) tileEntity;
				if (tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z) {
					shapeProvider = projector.getShapeProvider();
					isDoubleSided = projector.isDoubleSided;
					vTranslation = new VectorI(projector);
					// TODO vMin = projector.vMin;
					// TODO vMax = projector.vMax;
					// TODO upgrades ?
				} else {
					if ((((TileEntityForceFieldProjector) tileEntity).isEnabled)
						&& (((TileEntityForceFieldProjector) tileEntity).isCalculated())
						&& (((TileEntityForceFieldProjector) tileEntity).isValid())) {
						projectors.add((TileEntityForceFieldProjector) tileEntity);
					}
				}
			}
			// upgrade blocks (namely, relays)
			if (tileEntity instanceof IForceFieldUpgrade) {
				IForceFieldUpgradeEffector upgradeEffector = ((IForceFieldUpgrade)tileEntity).getUpgradeEffector();
				if (upgradeEffector != null) {
					Float currentValue = upgradeValues.get(upgradeEffector);
					if (currentValue == null) {
						currentValue = 0.0F;
					}
					upgradeValues.put(upgradeEffector, currentValue + ((IForceFieldUpgrade)tileEntity).getUpgradeValue());
					
					// camouflage identification
					if (upgradeEffector == EnumForceFieldUpgrade.CAMOUFLAGE) {
						Block blockCandidate = tileEntity.getWorldObj().getBlock(tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
						if (isValidCamouflage(blockCandidate)) {
							blockCamouflage = blockCandidate;
							metadataCamouflage = tileEntity.getWorldObj().getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
							lightCamouflage = blockCandidate.getLightValue(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
						}
					}
				}
			}
		}
		
		// set default coefficients, depending on projector
		scanSpeed = upgradeValues.get(EnumForceFieldUpgrade.SPEED) != null ? FORCEFIELD_MAX_SCAN_SPEED : FORCEFIELD_BASE_SCAN_SPEED;
		placeSpeed = upgradeValues.get(EnumForceFieldUpgrade.SPEED) != null ? FORCEFIELD_MAX_PLACE_SPEED : FORCEFIELD_BASE_PLACE_SPEED;
		startupEnergyCost = 60.0F + 20.0F * tier;
		scanEnergyCost = 1.0F + 1.0F * tier;
		placeEnergyCost = 3.0F + 3.0F * tier;
		entityEnergyCost = 2.0F;
		
		// apply scaling
		float speedRatio;
		for (Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgradeValues.entrySet()) {
			float scaledValue = entry.getKey().getScaledValue(1.0F, entry.getValue());
			if (scaledValue != 0.0F) {
				upgrades.put(entry.getKey(), scaledValue);
				
				speedRatio = entry.getKey().getScanSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					scanSpeed *= speedRatio;
				}
				speedRatio = entry.getKey().getPlaceSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					placeSpeed *= speedRatio;
				}
				
				startupEnergyCost += entry.getKey().getStartupEnergyCost(scaledValue);
				scanEnergyCost += entry.getKey().getScanEnergyCost(scaledValue);
				placeEnergyCost += entry.getKey().getPlaceEnergyCost(scaledValue);
				entityEnergyCost +=  entry.getKey().getEntityEffectEnergyCost(scaledValue);
			}
		}
		scanSpeed = Math.min(FORCEFIELD_MAX_SCAN_SPEED, scanSpeed);
		placeSpeed = Math.min(FORCEFIELD_MAX_PLACE_SPEED, placeSpeed);
		
		// fusion, inversion and stabilize just needs to be defined
		hasFusion = getScaledUpgrade(EnumForceFieldUpgrade.FUSION) > 0.0F;
		isInverted = getScaledUpgrade(EnumForceFieldUpgrade.INVERT) > 0.0F;
		hasStabilize = getScaledUpgrade(EnumForceFieldUpgrade.STABILIZE) > 0.0F;
		
		// temperature is a compound of cooling and warming
		temperatureLevel = getScaledUpgrade(EnumForceFieldUpgrade.WARM) - getScaledUpgrade(EnumForceFieldUpgrade.COOL);
		
		// disintegration, pump and thickness is the actual value
		disintegrationLevel = getScaledUpgrade(EnumForceFieldUpgrade.BREAK);
		maxPumpViscosity = getScaledUpgrade(EnumForceFieldUpgrade.PUMP);
		thickness = 1.0F + getScaledUpgrade(EnumForceFieldUpgrade.THICKNESS);
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			x, y, z,
			vMin.x, vMin.y, vMin.z,
			vMax.x, vMax.y, vMax.z);
	}
	
	public int onEntityEffect(World world, final int x, final int y, final int z, Entity entity) {
		int countdown = 0;
		for (Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgrades.entrySet()) {
			countdown += entry.getKey().onEntityEffect(entry.getValue(), world, x, y, z, entity);
		}
		return countdown;
	}
	
	public int applyDamages(World world, int x, int y, int z, DamageSource damageSource, int damageParameter, Vector3 damageDirection, int damageLevel) {
		// TODO
		return 0;
	}
}
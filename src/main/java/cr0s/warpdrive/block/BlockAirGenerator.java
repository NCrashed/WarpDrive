package cr0s.warpdrive.block;

import java.util.Random;

import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.client.ClientProxy;
import defense.api.IEMPBlock;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IAirCanister;
import cr0s.warpdrive.config.WarpDriveConfig;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockAirGenerator extends BlockAbstractRotatingContainer {
	public static final PropertyBool ACTIVE = PropertyBool.create("active");
	
	public BlockAirGenerator(final String registryName) {
		super(registryName, Material.IRON);
		setUnlocalizedName("warpdrive.machines.AirGenerator");
		GameRegistry.registerTileEntity(TileEntityAirGenerator.class, WarpDrive.PREFIX + registryName);
		
		setDefaultState(getDefaultState().withProperty(ACTIVE, false));
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, ACTIVE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(int metadata) {
		return getDefaultState()
				.withProperty(FACING, EnumFacing.getFront(metadata & 7))
				.withProperty(ACTIVE, (metadata & 8) != 0);
	}
	
	@Override
	public int getMetaFromState(IBlockState blockState) {
		return blockState.getValue(FACING).getIndex() + (blockState.getValue(ACTIVE) ? 8 : 0);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull World world, int metadata) {
		return new TileEntityAirGenerator();
	}
	
	@Nonnull
	@Override
	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase entityLiving) {
		EnumFacing enumFacing = getFacingFromEntity(pos, entityLiving).getOpposite();
		return this.getDefaultState().withProperty(FACING, enumFacing);
	}
	
	@Override
	public int quantityDropped(Random random) {
		return 1;
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos blockPos, IBlockState blockState, EntityPlayer entityPlayer, EnumHand hand, @Nullable ItemStack itemStackHeld, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote || hand == EnumHand.OFF_HAND) {
			return false;
		}
		
		TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAirGenerator) {
			TileEntityAirGenerator airGenerator = (TileEntityAirGenerator)tileEntity;
			if (itemStackHeld == null) {
				WarpDrive.addChatMessage(entityPlayer, airGenerator.getStatus());
				return true;
			} else {
				Item itemHeld = itemStackHeld.getItem();
				if (itemHeld instanceof IAirCanister) {
					IAirCanister airCanister = (IAirCanister) itemHeld;
					if (airCanister.canContainAir(itemStackHeld) && airGenerator.consumeEnergy(WarpDriveConfig.AIRGEN_ENERGY_PER_CANISTER, true)) {
						entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1);
						ItemStack toAdd = airCanister.fullDrop(itemStackHeld);
						if (toAdd != null) {
							if (!entityPlayer.inventory.addItemStackToInventory(toAdd)) {
								EntityItem ie = new EntityItem(entityPlayer.worldObj, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, toAdd);
								entityPlayer.worldObj.spawnEntityInWorld(ie);
							}
							((EntityPlayerMP)entityPlayer).sendContainerToPlayer(entityPlayer.inventoryContainer);
							airGenerator.consumeEnergy(WarpDriveConfig.AIRGEN_ENERGY_PER_CANISTER, false);
						}
					}
				}
			}
		}
		
		return false;
	}
}

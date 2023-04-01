package mcjty.xnet.modules.cables.blocks;

import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.ContainerFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.ManualEntry;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.lib.varia.EnergyTools;
import mcjty.rftoolsbase.api.xnet.channels.IConnectable;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.blocks.FacadeBlockItem;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.various.blocks.RedstoneProxyBlock;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.setup.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static mcjty.lib.builder.TooltipBuilder.*;

public class ConnectorBlock extends GenericCableBlock implements ITooltipSettings {

    public static final ManualEntry MANUAL = ManualHelper.create("xnet:simple/connector");
    private final Lazy<TooltipBuilder> tooltipBuilder = () -> new TooltipBuilder()
            .info(key("message.xnet.shiftmessage"))
            .infoShift(header(), gold(stack -> isAdvancedConnector()),
                    parameter("info", stack -> Integer.toString(isAdvancedConnector() ? Config.maxRfAdvancedConnector.get() : Config.maxRfConnector.get())));

    public ConnectorBlock(CableBlockType type) {
        super(Material.METAL, type);
    }

    @Override
    public ManualEntry getManualEntry() {
        return MANUAL;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ConnectorTileEntity();
    }

    @Override
    @Nonnull
    public ActionResultType use(@Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand handIn, @Nonnull BlockRayTraceResult hit) {
        if (!world.isClientSide) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof GenericTileEntity) {
                GenericTileEntity genericTileEntity = (GenericTileEntity) te;
                NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
                    @Override
                    @Nonnull
                    public ITextComponent getDisplayName() {
                        return new StringTextComponent("连接器");
                    }

                    @Nonnull
                    @Override
                    public Container createMenu(int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player) {
                        return new GenericContainer(CableModule.CONTAINER_CONNECTOR, id, ContainerFactory.EMPTY, genericTileEntity, player);
                    }
                }, pos);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void playerDestroy(@Nonnull World worldIn, @Nonnull PlayerEntity player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable TileEntity te, @Nonnull ItemStack stack) {
        if (te instanceof ConnectorTileEntity) {
            // If we are in mimic mode then the drop will be the facade as the connector will remain there
            ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) te;
            if (connectorTileEntity.getMimicBlock() != null) {
                ItemStack item = new ItemStack(FacadeModule.FACADE.get());
                FacadeBlockItem.setMimicBlock(item, connectorTileEntity.getMimicBlock());
                connectorTileEntity.setMimicBlock(null);
                popResource(worldIn, pos, item);
                return;
            }
        }
        super.playerDestroy(worldIn, player, pos, state, te, stack);
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
        TileEntity te = world.getBlockEntity(pos);
        if (te instanceof ConnectorTileEntity) {
            ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) te;
            if (connectorTileEntity.getMimicBlock() == null) {
                this.playerWillDestroy(world, pos, state, player);
                return world.setBlock(pos, Blocks.AIR.defaultBlockState(), world.isClientSide ? 11 : 3);
            } else {
                // We are in mimic mode. Don't remove the connector
                this.playerWillDestroy(world, pos, state, player);
                if (player.abilities.instabuild) {
                    connectorTileEntity.setMimicBlock(null);
                }
            }
        } else {
            return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        checkRedstone(world, pos);
        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
    }

    @Override
    public void onNeighborChange(BlockState state, IWorldReader blockAccess, BlockPos pos, BlockPos neighbor) {
        if (blockAccess instanceof World) {
            World world = (World) blockAccess;
            if (!world.isClientSide) {
                TileEntity te = world.getBlockEntity(pos);
                if (te instanceof ConnectorTileEntity) {
                    ConnectorTileEntity connector = (ConnectorTileEntity) te;
                    connector.possiblyMarkNetworkDirty(neighbor);
                }
            }
        }
        super.onNeighborChange(state, blockAccess, pos, neighbor);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side) {
        return false;
    }

    private void checkRedstone(World world, BlockPos pos) {
        TileEntity te = world.getBlockEntity(pos);
        if (te instanceof ConnectorTileEntity) {
//            int powered = world.isBlockIndirectlyGettingPowered(pos);
            int powered = world.getBestNeighborSignal(pos); // @todo 1.14 check
            ConnectorTileEntity genericTileEntity = (ConnectorTileEntity) te;
            genericTileEntity.setPowerInput(powered);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(@Nonnull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    protected int getRedstoneOutput(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        TileEntity te = world.getBlockEntity(pos);
        if (state.getBlock() instanceof ConnectorBlock && te instanceof ConnectorTileEntity) {
            ConnectorTileEntity connector = (ConnectorTileEntity) te;
            return connector.getPowerOut(side.getOpposite());
        }
        return 0;
    }

    @Override
    protected ConnectorType getConnectorType(@Nonnull CableColor color, IBlockReader world, BlockPos connectorPos, Direction facing) {
        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if ((block instanceof NetCableBlock || block instanceof ConnectorBlock) && state.getValue(COLOR) == color) {
            return ConnectorType.CABLE;
        } else if (isConnectable(world, connectorPos, facing) && color != CableColor.ROUTING) {
            return ConnectorType.BLOCK;
        } else if (isConnectableRouting(world, pos) && color == CableColor.ROUTING) {
            return ConnectorType.BLOCK;
        } else {
            return ConnectorType.NONE;
        }
    }

    public static boolean isConnectableRouting(IBlockReader world, BlockPos pos) {
        TileEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te instanceof TileEntityRouter || te instanceof TileEntityWirelessRouter) {
            return true;
        }
        return false;
    }

    public static boolean isConnectable(IBlockReader world, BlockPos connectorPos, Direction facing) {

        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isAir(state, world, pos)) {
            return false;
        }

        TileEntity tileEntity = world.getBlockEntity(connectorPos);
        if (!(tileEntity instanceof ConnectorTileEntity)) {
            return false;
        }
        ConnectorTileEntity connectorTE = (ConnectorTileEntity) tileEntity;

        if (!connectorTE.isEnabled(facing)) {
            return false;
        }


        TileEntity te = world.getBlockEntity(pos);

        if (block instanceof IConnectable) {
            IConnectable.ConnectResult result = ((IConnectable) block).canConnect(world, connectorPos, pos, te, facing);
            if (result == IConnectable.ConnectResult.NO) {
                return false;
            } else if (result == IConnectable.ConnectResult.YES) {
                return true;
            }
        }
        for (IConnectable connectable : XNet.xNetApi.getConnectables()) {
            IConnectable.ConnectResult result = connectable.canConnect(world, connectorPos, pos, te, facing);
            if (result == IConnectable.ConnectResult.NO) {
                return false;
            } else if (result == IConnectable.ConnectResult.YES) {
                return true;
            }
        }

        if (block instanceof ConnectorBlock) {
            return false;
        }
        if (block instanceof RedstoneProxyBlock || block == Blocks.REDSTONE_LAMP ||
                block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
            return true;
        }
        if (block.canConnectRedstone(state, world, pos, null) || state.isSignalSource()) {
            return true;
        }
        if (te == null) {
            return false;
        }
        if (EnergyTools.isEnergyTE(te, null)) {
            return true;
        }
        if (te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
            return true;
        }
        if (te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).isPresent()) {
            return true;
        }
        if (te instanceof TileEntityController) {
            return true;
        }
        if (te instanceof TileEntityRouter) {
            return true;
        }
        return false;
    }

    // @todo 1.14
//    @Override
//    @SideOnly(Side.CLIENT)
//    public boolean shouldSideBeRendered(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side) {
//        BlockState mimicBlock = getMimicBlock(blockAccess, pos);
//        if (mimicBlock == null) {
//            return false;
//        } else {
//            return mimicBlock.shouldSideBeRendered(blockAccess, pos, side);
//        }
//    }

    // @todo 1.15
//    @Override
//    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
//        return true;    // delegated to GenericCableBakedModel#getQuads
//    }
//
    @Nonnull
    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootContext.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        ServerWorld world = builder.getLevel();
        for (ItemStack drop : drops) {
            WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);
            ConsumerId consumer = worldBlob.getConsumerAt(new BlockPos(builder.getOptionalParameter(LootParameters.ORIGIN)));
            if (consumer != null) {
                drop.getOrCreateTag().putInt("consumerId", consumer.getId());
            }
        }
        return drops;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable IBlockReader worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltipBuilder.get().makeTooltip(getRegistryName(), stack, tooltip, flagIn);
    }

    @Override
    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        ConsumerId consumer;
        if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("consumerId")) {
            consumer = new ConsumerId(stack.getTag().getInt("consumerId"));
        } else {
            XNetBlobData blobData = XNetBlobData.get(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            consumer = worldBlob.newConsumer();
        }
        createCableSegment(world, pos, consumer);
    }

    public void createCableSegment(World world, BlockPos pos, ConsumerId consumer) {
        XNetBlobData blobData = XNetBlobData.get(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).getValue(COLOR);
        worldBlob.createNetworkConsumer(pos, new ColorId(color.ordinal() + 1), consumer);
        blobData.save();
    }

    public static boolean isAdvancedConnector(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof GenericCableBlock) {
            return ((GenericCableBlock) block).isAdvancedConnector();
        }
        return false;
    }

    @Override
    public boolean isAdvancedConnector() {
        return false;
    }
}

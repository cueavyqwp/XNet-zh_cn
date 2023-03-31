package mcjty.xnet.modules.various.items;

import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ConnectorUpgradeItem extends Item {

    public ConnectorUpgradeItem() {
        super(new Properties()
                .tab(XNet.setup.getTab())
        );
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new StringTextComponent(TextFormatting.BLUE + "对连接器潜行右键以将其升级"));
        //用不着三行
        //用不着三行
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> use(@Nonnull World worldIn, @Nonnull PlayerEntity playerIn, @Nonnull Hand hand) {
        return super.use(worldIn, playerIn, hand);
    }

    @Override
    @Nonnull
    public ActionResultType useOn(@Nonnull ItemUseContext context) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        Block block = state.getBlock();

        if (block == CableModule.CONNECTOR.get()) {
            if (!world.isClientSide) {
                TileEntity te = world.getBlockEntity(pos);
                if (te instanceof ConnectorTileEntity) {
                    CompoundNBT tag = new CompoundNBT();
                    te.save(tag);
                    CableColor color = world.getBlockState(pos).getValue(GenericCableBlock.COLOR);

                    XNetBlobData blobData = XNetBlobData.get(world);
                    WorldBlob worldBlob = blobData.getWorldBlob(world);
                    ConsumerId consumer = worldBlob.getConsumerAt(pos);
                    ((ConnectorBlock)block).unlinkBlock(world, pos);
                    world.setBlockAndUpdate(pos, CableModule.ADVANCED_CONNECTOR.get().defaultBlockState().setValue(GenericCableBlock.COLOR, color));
                    BlockState blockState = world.getBlockState(pos);
                    ((ConnectorBlock)blockState.getBlock()).createCableSegment(world, pos, consumer);

                    blockState = ((ConnectorBlock) block).calculateState(world, pos, blockState);
                    world.setBlock(pos, blockState, Constants.BlockFlags.BLOCK_UPDATE + Constants.BlockFlags.NOTIFY_NEIGHBORS);
                    player.inventory.removeItem(player.inventory.selected, 1);
                    player.containerMenu.broadcastChanges();
                    player.displayClientMessage(new StringTextComponent(TextFormatting.GREEN + "连接器已升级"), false);
                }
            }
            return ActionResultType.SUCCESS;
        } else if (block == CableModule.ADVANCED_CONNECTOR.get()) {
            if (!world.isClientSide) {
                player.displayClientMessage(new StringTextComponent(TextFormatting.YELLOW + "此连接器已经升过级了!"), false);
            }
            return ActionResultType.SUCCESS;
        } else {
            if (!world.isClientSide) {
                player.displayClientMessage(new StringTextComponent(TextFormatting.RED + "使用其在连接器上以进行升级!"), false);
            }
            return ActionResultType.SUCCESS;
        }
    }

}

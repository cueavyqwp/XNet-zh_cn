package mcjty.xnet.setup;

import mcjty.lib.container.EmptyContainer;
import mcjty.lib.blocks.GenericBlock;
import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.cables.ConnectorTileEntity;
import mcjty.xnet.blocks.cables.GuiConnector;
import mcjty.xnet.items.manual.GuiXNetManual;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {

    public static final int GUI_MANUAL_XNET = 0;
    public static final int GUI_CONTROLLER = 1;
    public static final int GUI_CONNECTOR = 2;
    public static final int GUI_ROUTER = 3;
    public static final int GUI_WIRELESS_ROUTER = 4;
    public static final String SHIFT_MESSAGE = "<Press Shift>";

    @Override
    public Object getServerGuiElement(int guiid, EntityPlayer entityPlayer, World world, int x, int y, int z) {
        if (guiid == GUI_MANUAL_XNET) {
            return null;
        }
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof GenericBlock) {
            GenericBlock<?, ?> genericBlock = (GenericBlock<?, ?>) block;
            TileEntity te = world.getTileEntity(pos);
            return genericBlock.createServerContainer(entityPlayer, te);
        } else if (block instanceof ConnectorBlock) {
            return new EmptyContainer(entityPlayer, null);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int guiid, EntityPlayer entityPlayer, World world, int x, int y, int z) {
        if (guiid == GUI_MANUAL_XNET) {
            return new GuiXNetManual(GuiXNetManual.MANUAL_XNET);
        }
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof GenericBlock) {
            GenericBlock<?, ?> genericBlock = (GenericBlock<?, ?>) block;
            TileEntity te = world.getTileEntity(pos);
            return genericBlock.createClientGui(entityPlayer, te);
        } else if (block instanceof ConnectorBlock) {
            TileEntity te = world.getTileEntity(pos);
            return new GuiConnector((ConnectorTileEntity) te, new EmptyContainer(entityPlayer, null));
        }
        return null;
    }
}

package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.ICommandHandler;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.network.TypedMapTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.blocks.controller.TileEntityController;
import mcjty.xnet.clientinfo.ConnectedBlockClientInfo;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.List;
import java.util.function.Supplier;

public class PacketGetConnectedBlocks implements IMessage {

    protected BlockPos pos;
    protected TypedMap params;

    public PacketGetConnectedBlocks() {
    }

    public PacketGetConnectedBlocks(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketGetConnectedBlocks(BlockPos pos) {
        this.pos = pos;
        this.params = TypedMap.EMPTY;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = NetworkTools.readPos(buf);
        params = TypedMapTools.readArguments(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writePos(buf, pos);
        TypedMapTools.writeArguments(buf, params);
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            TileEntity te = ctx.getSender().getEntityWorld().getTileEntity(pos);
            ICommandHandler commandHandler = (ICommandHandler) te;
            List<ConnectedBlockClientInfo> list = commandHandler.executeWithResultList(TileEntityController.CMD_GETCONNECTEDBLOCKS, params, Type.create(ConnectedBlockClientInfo.class));
            XNetMessages.INSTANCE.sendTo(new PacketConnectedBlocksReady(pos, TileEntityController.CLIENTCMD_CONNECTEDBLOCKSREADY, list), ctx.getSender());
        });
        ctx.setPacketHandled(true);
    }
}

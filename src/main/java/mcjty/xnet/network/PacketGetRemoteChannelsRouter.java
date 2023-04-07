package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.ICommandHandler;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.network.TypedMapTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.blocks.router.TileEntityRouter;
import mcjty.xnet.clientinfo.ControllerChannelClientInfo;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.List;
import java.util.function.Supplier;

public class PacketGetRemoteChannelsRouter implements IMessage {

    protected BlockPos pos;
    protected TypedMap params;

    public PacketGetRemoteChannelsRouter() {
    }

    public PacketGetRemoteChannelsRouter(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketGetRemoteChannelsRouter(BlockPos pos) {
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
            List<ControllerChannelClientInfo> list = commandHandler.executeWithResultList(TileEntityRouter.CMD_GETREMOTECHANNELS, params, Type.create(ControllerChannelClientInfo.class));
            XNetMessages.INSTANCE.sendTo(new PacketRemoteChannelsRouterReady(pos, TileEntityRouter.CLIENTCMD_CHANNELSREMOTEREADY, list), ctx.getSender());
        });
        ctx.setPacketHandled(true);
    }
}

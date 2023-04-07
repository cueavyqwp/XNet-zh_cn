package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.IClientCommandHandler;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.lib.typed.Type;
import mcjty.lib.varia.Logging;
import mcjty.xnet.XNet;
import mcjty.xnet.clientinfo.ControllerChannelClientInfo;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketRemoteChannelsRouterReady implements IMessage {

    public BlockPos pos;
    public List<ControllerChannelClientInfo> list;
    public String command;

    public PacketRemoteChannelsRouterReady() {
    }

    public PacketRemoteChannelsRouterReady(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketRemoteChannelsRouterReady(BlockPos pos, String command, List<ControllerChannelClientInfo> list) {
        this.pos = pos;
        this.command = command;
        this.list = new ArrayList<>();
        this.list.addAll(list);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = NetworkTools.readPos(buf);
        command = NetworkTools.readString(buf);

        int size = buf.readInt();
        if (size != -1) {
            list = new ArrayList<>(size);
            for (int i = 0 ; i < size ; i++) {
                ControllerChannelClientInfo result;
                if (buf.readBoolean()) {
                    result = new ControllerChannelClientInfo(buf);
                } else {
                    result = null;
                }
                list.add(result);
            }
        } else {
            list = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writePos(buf, pos);
        NetworkTools.writeString(buf, command);

        if (list == null) {
            buf.writeInt(-1);
        } else {
            buf.writeInt(list.size());
            for (ControllerChannelClientInfo item : list) {
                if (item == null) {
                    buf.writeBoolean(false);
                } else {
                    buf.writeBoolean(true);
                    item.writeToNBT(buf);
                }
            }
        }
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            TileEntity te = XNet.proxy.getClientWorld().getTileEntity(pos);
            IClientCommandHandler clientCommandHandler = (IClientCommandHandler) te;
            if (!clientCommandHandler.receiveListFromServer(command, list, Type.create(ControllerChannelClientInfo.class))) {
                Logging.log("Command " + command + " was not handled!");
            }
        });
        ctx.setPacketHandled(true);
    }
}

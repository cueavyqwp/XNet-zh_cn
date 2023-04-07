package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.xnet.blocks.controller.gui.GuiController;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketJsonToClipboard implements IMessage {

    private String json;

    @Override
    public void fromBytes(ByteBuf buf) {
        json = NetworkTools.readStringUTF8(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writeStringUTF8(buf, json);
    }

    public PacketJsonToClipboard() {
    }

    public PacketJsonToClipboard(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketJsonToClipboard(String json) {
        this.json = json;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            GuiController.toClipboard(json);
        });
        ctx.setPacketHandled(true);
    }
}

package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.thirteen.Context;
import mcjty.xnet.blocks.controller.gui.GuiController;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.function.Supplier;

public class PacketControllerError implements IMessage {

    private String error;

    @Override
    public void fromBytes(ByteBuf buf) {
        error = NetworkTools.readStringUTF8(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writeStringUTF8(buf, error);
    }

    public PacketControllerError() {
    }

    public PacketControllerError(ByteBuf buf) {
        fromBytes(buf);
    }

    public PacketControllerError(String error) {
        this.error = error;
    }

    public void handle(Supplier<Context> supplier) {
        Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            GuiController.showError(error);
        });
        ctx.setPacketHandled(true);
    }
}

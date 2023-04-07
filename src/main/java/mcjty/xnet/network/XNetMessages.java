package mcjty.xnet.network;

import mcjty.lib.network.PacketHandler;
import mcjty.lib.thirteen.ChannelBuilder;
import mcjty.lib.thirteen.SimpleChannel;
import mcjty.xnet.XNet;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class XNetMessages {
    public static SimpleNetworkWrapper INSTANCE;

    public static void registerMessages(String name) {
        SimpleChannel net = ChannelBuilder
                .named(new ResourceLocation(XNet.MODID, name))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net.getNetwork();

        // Server side
        net.registerMessageServer(id(), PacketGetChannels.class, PacketGetChannels::toBytes, PacketGetChannels::new, PacketGetChannels::handle);
        net.registerMessageServer(id(), PacketGetLocalChannelsRouter.class, PacketGetLocalChannelsRouter::toBytes, PacketGetLocalChannelsRouter::new, PacketGetLocalChannelsRouter::handle);
        net.registerMessageServer(id(), PacketGetRemoteChannelsRouter.class, PacketGetRemoteChannelsRouter::toBytes, PacketGetRemoteChannelsRouter::new, PacketGetRemoteChannelsRouter::handle);
        net.registerMessageServer(id(), PacketGetConnectedBlocks.class, PacketGetConnectedBlocks::toBytes, PacketGetConnectedBlocks::new, PacketGetConnectedBlocks::handle);

        // Client side
        net.registerMessageClient(id(), PacketChannelsReady.class, PacketChannelsReady::toBytes, PacketChannelsReady::new, PacketChannelsReady::handle);
        net.registerMessageClient(id(), PacketLocalChannelsRouterReady.class, PacketLocalChannelsRouterReady::toBytes, PacketLocalChannelsRouterReady::new, PacketLocalChannelsRouterReady::handle);
        net.registerMessageClient(id(), PacketRemoteChannelsRouterReady.class, PacketRemoteChannelsRouterReady::toBytes, PacketRemoteChannelsRouterReady::new, PacketRemoteChannelsRouterReady::handle);
        net.registerMessageClient(id(), PacketConnectedBlocksReady.class, PacketConnectedBlocksReady::toBytes, PacketConnectedBlocksReady::new, PacketConnectedBlocksReady::handle);
        net.registerMessageClient(id(), PacketJsonToClipboard.class, PacketJsonToClipboard::toBytes, PacketJsonToClipboard::new, PacketJsonToClipboard::handle);
        net.registerMessageClient(id(), PacketControllerError.class, PacketControllerError::toBytes, PacketControllerError::new, PacketControllerError::handle);
    }

    private static int id() {
        return PacketHandler.nextPacketID();
    }
}

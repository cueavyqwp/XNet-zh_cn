package mcjty.xnet.clientinfo;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class ControllerChannelClientInfo {
    @Nonnull private final String channelName;
    @Nonnull private final String publishedName;
    @Nonnull private final BlockPos pos;
    @Nonnull private final IChannelType channelType;
    private final boolean remote;      // If this channel was made available through a wireless router
    private final int index;        // Index of the channel within that controller (0 through 7)

    public ControllerChannelClientInfo(@Nonnull String channelName, @Nonnull String publishedName, @Nonnull BlockPos pos, @Nonnull IChannelType channelType, boolean remote, int index) {
        this.channelName = channelName;
        this.publishedName = publishedName;
        this.pos = pos;
        this.channelType = channelType;
        this.remote = remote;
        this.index = index;
    }

    public ControllerChannelClientInfo(@Nonnull ByteBuf buf) {
        channelName = NetworkTools.readStringUTF8(buf);
        publishedName = NetworkTools.readStringUTF8(buf);
        String id = NetworkTools.readString(buf);
        IChannelType t = XNet.xNetApi.findType(id);
        if (t == null) {
            throw new RuntimeException("Bad type: " + id);
        }
        channelType = t;
        pos = NetworkTools.readPos(buf);
        remote = buf.readBoolean();
        index = buf.readInt();
    }

    public void writeToNBT(@Nonnull ByteBuf buf) {
        NetworkTools.writeStringUTF8(buf, channelName);
        NetworkTools.writeStringUTF8(buf, publishedName);
        NetworkTools.writeString(buf, channelType.getID());
        NetworkTools.writePos(buf, pos);
        buf.writeBoolean(remote);
        buf.writeInt(index);
    }

    @Nonnull
    public String getChannelName() {
        return channelName;
    }

    @Nonnull
    public String getPublishedName() {
        return publishedName;
    }

    @Nonnull
    public BlockPos getPos() {
        return pos;
    }

    @Nonnull
    public IChannelType getChannelType() {
        return channelType;
    }

    public int getIndex() {
        return index;
    }

    public boolean isRemote() {
        return remote;
    }
}

package mcjty.xnet.multiblock;

import mcjty.xnet.api.channels.IChannelType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class WirelessChannelKey {
    @Nonnull private final String name;
    @Nonnull private final IChannelType channelType;
    @Nullable private final UUID owner;

    public WirelessChannelKey(@Nonnull String name, @Nonnull IChannelType channelType, @Nullable UUID owner) {
        this.name = name;
        this.channelType = channelType;
        this.owner = owner;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public IChannelType getChannelType() {
        return channelType;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WirelessChannelKey that = (WirelessChannelKey) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, channelType, owner);
    }

    @Override
    public String toString() {
        return "WirelessChannelKey{" +
                "name='" + name + '\'' +
                ", channelType=" + channelType +
                ", owner=" + owner +
                '}';
    }
}

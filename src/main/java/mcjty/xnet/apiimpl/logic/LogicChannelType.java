package mcjty.xnet.apiimpl.logic;

import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.channels.IConnectorSettings;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LogicChannelType implements IChannelType {

    @Override
    public String getID() {
        return "逻辑";
    }

    @Override
    public String getName() {
        return "逻辑";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        return true;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull EnumFacing side) {
        return new LogicConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new LogicChannelSettings();
    }
}

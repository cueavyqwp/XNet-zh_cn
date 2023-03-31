package mcjty.xnet.apiimpl.fluids;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidChannelType implements IChannelType {

    @Override
    public String getID() {
        return "流体";
    }

    @Override
    public String getName() {
        return "流体";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction side) {
        TileEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull Direction side) {
        return new FluidConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new FluidChannelSettings();
    }
}

package mcjty.xnet.apiimpl.energy;

import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.channels.IConnectorSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyChannelType implements IChannelType {

    @Override
    public String getID() {
        return "xnet.energy";
    }

    @Override
    public String getName() {
        return "Energy";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return false;
        }
        if (te.hasCapability(CapabilityEnergy.ENERGY, side)) {
            return true;
        }
//        if (te instanceof IInventory) {
//            return true;
//        }
        return false;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull EnumFacing side) {
        return new EnergyConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new EnergyChannelSettings();
    }
}

package mcjty.xnet.apiimpl.energy;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyChannelType implements IChannelType {

    @Override
    public String getID() {
        return "能量";
    }

    @Override
    public String getName() {
        return "能量";
    }

    @Override
    public boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable Direction side) {
        TileEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te.getCapability(CapabilityEnergy.ENERGY, side).isPresent()) {
            return true;
        }
//        if (te instanceof IInventory) {
//            return true;
//        }
        return false;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull Direction side) {
        return new EnergyConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new EnergyChannelSettings();
    }
}

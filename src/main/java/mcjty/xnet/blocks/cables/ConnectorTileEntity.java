package mcjty.xnet.blocks.cables;

import mcjty.lib.bindings.DefaultValue;
import mcjty.lib.bindings.IValue;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.api.tiles.IConnectorTile;
import mcjty.xnet.blocks.facade.IFacadeSupport;
import mcjty.xnet.blocks.facade.MimicBlockSupport;
import mcjty.xnet.config.ConfigSetup;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;

public class ConnectorTileEntity extends GenericTileEntity implements IFacadeSupport, IConnectorTile {

    public static final String CMD_ENABLE = "connector.enable";
    public static final Key<Integer> PARAM_FACING = new Key<>("facing", Type.INTEGER);
    public static final Key<Boolean> PARAM_ENABLED = new Key<>("enabled", Type.BOOLEAN);

    private MimicBlockSupport mimicBlockSupport = new MimicBlockSupport();

    private int energy = 0;
    private int inputFromSide[] = new int[] { 0, 0, 0, 0, 0, 0 };
    private String name = "";

    // Count the number of redstone pulses we got
    private int pulseCounter;
    private int powerOut[] = new int[] { 0, 0, 0, 0, 0, 0 };

    private byte enabled = 0x3f;

    private Block[] cachedNeighbours = new Block[EnumFacing.VALUES.length];

    public static final Key<String> VALUE_NAME = new Key<>("name", Type.STRING);

    @Override
    public IValue<?>[] getValues() {
        return new IValue[] {
                new DefaultValue<>(VALUE_NAME, this::getConnectorName, this::setConnectorName),
        };
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        IBlockState oldMimicBlock = mimicBlockSupport.getMimicBlock();
        byte oldEnabled = enabled;

        super.onDataPacket(net, packet);

        if (world.isRemote) {
            // If needed send a render update.
            if (enabled != oldEnabled || mimicBlockSupport.getMimicBlock() != oldMimicBlock) {
                world.markBlockRangeForRenderUpdate(getPos(), getPos());
            }
        }
    }

    public int getPowerOut(EnumFacing side) {
        return powerOut[side.ordinal()];
    }

    public void setPowerOut(EnumFacing side, int powerOut) {
        if (powerOut > 15) {
            powerOut = 15;
        }
        if (this.powerOut[side.ordinal()] == powerOut) {
            return;
        }
        this.powerOut[side.ordinal()] = powerOut;
        markDirty();
        world.neighborChanged(pos.offset(side), this.getBlockType(), this.pos);
    }

    public void setEnabled(EnumFacing direction, boolean e) {
        if (e) {
            enabled |= 1 << direction.ordinal();
        } else {
            enabled &= ~(1 << direction.ordinal());
        }
        markDirtyClient();
    }

    public boolean isEnabled(EnumFacing direction) {
        return (enabled & (1 << direction.ordinal())) != 0;
    }

    @Override
    public IBlockState getMimicBlock() {
        return mimicBlockSupport.getMimicBlock();
    }

    public void setMimicBlock(IBlockState mimicBlock) {
        mimicBlockSupport.setMimicBlock(mimicBlock);
        markDirtyClient();
    }

    @Override
    public void setPowerInput(int powered) {
        if (powerLevel == 0 && powered > 0) {
            pulseCounter++;
        }
        super.setPowerInput(powered);
    }

    @Override
    public int getPulseCounter() {
        return pulseCounter;
    }

    // Optimization to only increase the network if there is an actual block change
    public void possiblyMarkNetworkDirty(@Nonnull BlockPos neighbor) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (getPos().offset(facing).equals(neighbor)) {
                Block newblock = world.getBlockState(neighbor).getBlock();
                if (newblock != cachedNeighbours[facing.ordinal()]) {
                    cachedNeighbours[facing.ordinal()] = newblock;
                    WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
                    worldBlob.markNetworkDirty(worldBlob.getNetworkAt(getPos()));
                }
                return;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        energy = tagCompound.getInteger("energy");
        inputFromSide = tagCompound.getIntArray("inputs");
        if (inputFromSide.length != 6) {
            inputFromSide = new int[] { 0, 0, 0, 0, 0, 0 };
        }
        mimicBlockSupport.readFromNBT(tagCompound);
        pulseCounter = tagCompound.getInteger("pulse");
        for (int i = 0 ; i < 6 ; i++) {
            powerOut[i] = tagCompound.getByte("p" + i);
        }
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        name = tagCompound.getString("name");
        if (tagCompound.hasKey("enabled")) {
            enabled = tagCompound.getByte("enabled");
        } else {
            enabled = 0x3f;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("energy", energy);
        tagCompound.setIntArray("inputs", inputFromSide);
        mimicBlockSupport.writeToNBT(tagCompound);
        tagCompound.setInteger("pulse", pulseCounter);
        for (int i = 0 ; i < 6 ; i++) {
            tagCompound.setByte("p" + i, (byte) powerOut[i]);
        }
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        tagCompound.setString("name", name);
        tagCompound.setByte("enabled", enabled);
    }

    @Override
    public int getPowerLevel() {
        return powerLevel;
    }

    public void setConnectorName(String n) {
        this.name = n;
        markDirtyClient();
    }


    public String getConnectorName() {
        return name;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        if (this.energy != energy) {
            if (energy < 0) {
                energy = 0;
            }
            this.energy = energy;
            markDirtyQuick();
        }
    }

    public void setEnergyInputFrom(EnumFacing from, int rate) {
        if (inputFromSide[from.ordinal()] != rate) {
            inputFromSide[from.ordinal()] = rate;
            markDirtyQuick();
        }
    }

    public int getMaxEnergy() {
        return ConfigSetup.maxRfConnector.get();
    }

    private int receiveEnergyInternal(EnumFacing from, int maxReceive, boolean simulate) {
        if (from == null) {
            return 0;
        }
        int m = inputFromSide[from.ordinal()];
        if (m > 0) {
            int toreceive = Math.min(maxReceive, m);
            int newenergy = energy + toreceive;
            if (newenergy > getMaxEnergy()) {
                toreceive -= newenergy - getMaxEnergy();
                newenergy = getMaxEnergy();
            }
            if (!simulate && energy != newenergy) {
                energy = newenergy;
                inputFromSide[from.ordinal()] = 0;
                markDirtyQuick();
            }
            return toreceive;
        }
        return 0;
    }

    private int getEnergyStoredInternal() {
        return energy;
    }

    private int getMaxEnergyStoredInternal() {
        return getMaxEnergy();
    }

    private IEnergyStorage[] sidedHandlers = new IEnergyStorage[6];

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, TypedMap params) {
        boolean rc = super.execute(playerMP, command, params);
        if (rc) {
            return true;
        }
        if (CMD_ENABLE.equals(command)) {
            int f = params.get(PARAM_FACING);
            boolean e = params.get(PARAM_ENABLED);
            setEnabled(EnumFacing.VALUES[f], e);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != null) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            if (facing == null) {
                return null;
            } else {
                if (sidedHandlers[facing.ordinal()] == null) {
                    createSidedHandler(facing);
                }
                return CapabilityEnergy.ENERGY.cast(sidedHandlers[facing.ordinal()]);
            }
        }
        return super.getCapability(capability, facing);
    }

    private void createSidedHandler(EnumFacing facing) {
        class SidedHandler implements IEnergyStorage {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return ConnectorTileEntity.this.receiveEnergyInternal(facing, maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return ConnectorTileEntity.this.getEnergyStoredInternal();
            }

            @Override
            public int getMaxEnergyStored() {
                return ConnectorTileEntity.this.getMaxEnergyStoredInternal();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        }
        sidedHandlers[facing.ordinal()] = new SidedHandler();
    }

}

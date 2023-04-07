package mcjty.xnet.apiimpl.energy;

import com.google.gson.JsonObject;
import mcjty.lib.varia.EnergyTools;
import mcjty.lib.varia.WorldTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.channels.IControllerContext;
import mcjty.xnet.api.gui.IEditorGui;
import mcjty.xnet.api.gui.IndicatorIcon;
import mcjty.xnet.api.helper.DefaultChannelSettings;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.cables.ConnectorTileEntity;
import mcjty.xnet.config.ConfigSetup;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnergyChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    // Cache data
    private List<Pair<SidedConsumer, EnergyConnectorSettings>> energyExtractors = null;
    private List<Pair<SidedConsumer, EnergyConnectorSettings>> energyConsumers = null;

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
    }


    @Override
    public void readFromNBT(NBTTagCompound tag) {
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        updateCache(channel, context);

        World world = context.getControllerWorld();

        // First find out how much energy we have to distribute in total
        int totalToDistribute = 0;
        // Keep track of the connectors we already got energy from and how much energy we
        // got from it
        Map<BlockPos, Integer> alreadyHandled = new HashMap<>();

        List<Pair<ConnectorTileEntity, Integer>> energyProducers = new ArrayList<>();
        for (Pair<SidedConsumer, EnergyConnectorSettings> entry : energyExtractors) {
            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (connectorPos != null) {

                EnumFacing side = entry.getKey().getSide();
                BlockPos energyPos = connectorPos.offset(side);
                if (!WorldTools.chunkLoaded(world, energyPos)) {
                    continue;
                }

                TileEntity te = world.getTileEntity(energyPos);
                // @todo report error somewhere?
                if (isEnergyTE(te, side.getOpposite())) {
                    EnergyConnectorSettings settings = entry.getValue();
                    ConnectorTileEntity connectorTE = (ConnectorTileEntity) world.getTileEntity(connectorPos);

                    if (checkRedstone(world, settings, connectorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Integer count = settings.getMinmax();
                    if (count != null) {
                        int level = getEnergyLevel(te, side.getOpposite());
                        if (level < count) {
                            continue;
                        }
                    }

                    Integer rate = settings.getRate();
                    if (rate == null) {
                        boolean advanced = ConnectorBlock.isAdvancedConnector(world, connectorPos);
                        rate = advanced ? ConfigSetup.maxRfRateAdvanced.get() : ConfigSetup.maxRfRateNormal.get();
                    }
                    connectorTE.setEnergyInputFrom(side, rate);

                    if (!alreadyHandled.containsKey(connectorPos)) {
                        // We did not handle this connector yet. Remember the amount of energy in it
                        alreadyHandled.put(connectorPos, connectorTE.getEnergy());
                    }

                    // Check how much energy we can still send from that connector
                    int connectorEnergy = alreadyHandled.get(connectorPos);
                    int tosend = Math.min(rate, connectorEnergy);
                    if (tosend > 0) {
                        // Decrease the energy from our temporary datastructure
                        alreadyHandled.put(connectorPos, connectorEnergy - tosend);
                        totalToDistribute += tosend;
                        energyProducers.add(Pair.of(connectorTE, tosend));
                    }
                }
            }
        }

        if (totalToDistribute <= 0) {
            // Nothing to do
            return;
        }

        if (!context.checkAndConsumeRF(ConfigSetup.controllerOperationRFT.get())) {
            // Not enough energy for this operation
            return;
        }

        int actuallyConsumed = insertEnergy(context, totalToDistribute);
        if (actuallyConsumed <= 0) {
            // Nothing was done
            return;
        }

        // Now we need to actually fetch the energy from the producers
        for (Pair<ConnectorTileEntity, Integer> entry : energyProducers) {
            ConnectorTileEntity connectorTE = entry.getKey();
            int amount = entry.getValue();

            int actuallySpent = Math.min(amount, actuallyConsumed);
            connectorTE.setEnergy(connectorTE.getEnergy() - actuallySpent);
            actuallyConsumed -= actuallySpent;
            if (actuallyConsumed <= 0) {
                break;
            }
        }
    }

    private int insertEnergy(@Nonnull IControllerContext context, int energy) {
        int total = 0;
        World world = context.getControllerWorld();
        for (Pair<SidedConsumer, EnergyConnectorSettings> entry : energyConsumers) {
            EnergyConnectorSettings settings = entry.getValue();
            BlockPos extractorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (extractorPos != null) {
                EnumFacing side = entry.getKey().getSide();
                BlockPos pos = extractorPos.offset(side);
                if (!WorldTools.chunkLoaded(world, pos)) {
                    continue;
                }
                TileEntity te = world.getTileEntity(pos);
                // @todo report error somewhere?
                if (isEnergyTE(te, settings.getFacing())) {

                    if (checkRedstone(world, settings, extractorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Integer count = settings.getMinmax();
                    if (count != null) {
                        int level = getEnergyLevel(te, settings.getFacing());
                        if (level >= count) {
                            continue;
                        }
                    }

                    Integer rate = settings.getRate();
                    if (rate == null) {
                        boolean advanced = ConnectorBlock.isAdvancedConnector(world, extractorPos);
                        rate = advanced ? ConfigSetup.maxRfRateAdvanced.get() : ConfigSetup.maxRfRateNormal.get();
                    }
                    int totransfer = Math.min(rate, energy);
                    long e = EnergyTools.receiveEnergy(te, settings.getFacing(), totransfer);
                    energy -= e;
                    total += e;
                    if (energy <= 0) {
                        return total;
                    }
                }
            }
        }
        return total;
    }


    public static boolean isEnergyTE(@Nullable TileEntity te, @Nonnull EnumFacing side) {
        if (te == null) {
            return false;
        }
        return te.hasCapability(CapabilityEnergy.ENERGY, side);
    }

    public static int getEnergyLevel(TileEntity tileEntity, @Nonnull EnumFacing side) {
        if (tileEntity != null && tileEntity.hasCapability(CapabilityEnergy.ENERGY, side)) {
            IEnergyStorage energy = tileEntity.getCapability(CapabilityEnergy.ENERGY, side);
            return energy.getEnergyStored();
        } else {
            return 0;
        }
    }



    @Override
    public void cleanCache() {
        energyExtractors = null;
        energyConsumers = null;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (energyExtractors == null) {
            energyExtractors = new ArrayList<>();
            energyConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                if (con.getEnergyMode() == EnergyConnectorSettings.EnergyMode.EXT) {
                    energyExtractors.add(Pair.of(entry.getKey(), con));
                } else {
                    energyConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                if (con.getEnergyMode() == EnergyConnectorSettings.EnergyMode.INS) {
                    energyConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            energyExtractors.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
            energyConsumers.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
        }
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 11, 80, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
    }

    @Override
    public void update(Map<String, Object> data) {
    }
}

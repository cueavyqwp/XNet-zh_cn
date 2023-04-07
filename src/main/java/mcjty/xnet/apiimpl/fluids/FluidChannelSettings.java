package mcjty.xnet.apiimpl.fluids;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.WorldTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.channels.IControllerContext;
import mcjty.xnet.api.gui.IEditorGui;
import mcjty.xnet.api.gui.IndicatorIcon;
import mcjty.xnet.api.helper.DefaultChannelSettings;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.config.ConfigSetup;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FluidChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";

    public enum ChannelMode {
        PRIORITY,
        DISTRIBUTE
    }

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay = 0;
    private int roundRobinOffset = 0;

    // Cache data
    private Map<SidedConsumer, FluidConnectorSettings> fluidExtractors = null;
    private List<Pair<SidedConsumer, FluidConnectorSettings>> fluidConsumers = null;

    public ChannelMode getChannelMode() {
        return channelMode;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add("mode", new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = EnumStringTranslators.getFluidChannelMode(data.get("mode").getAsString());
    }


    @Override
    public void readFromNBT(NBTTagCompound tag) {
        channelMode = ChannelMode.values()[tag.getByte("mode")];
        delay = tag.getInteger("delay");
        roundRobinOffset = tag.getInteger("offset");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setByte("mode", (byte) channelMode.ordinal());
        tag.setInteger("delay", delay);
        tag.setInteger("offset", roundRobinOffset);
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200*6;      // Multiply of the different speeds we have
        }
        if (delay % 10 != 0) {
            return;
        }
        int d = delay/10;

        updateCache(channel, context);
        // @todo optimize
        World world = context.getControllerWorld();
        extractorsLoop:
        for (Map.Entry<SidedConsumer, FluidConnectorSettings> entry : fluidExtractors.entrySet()) {
            FluidConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            BlockPos extractorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (extractorPos != null) {
                EnumFacing side = entry.getKey().getSide();
                BlockPos pos = extractorPos.offset(side);
                if (!WorldTools.chunkLoaded(world, pos)) {
                    continue;
                }

                TileEntity te = world.getTileEntity(pos);
                IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing());
                // @todo report error somewhere?
                if (handler != null) {
                    if (checkRedstone(world, settings, extractorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    FluidStack extractMatcher = settings.getMatcher();

                    int toextract = settings.getRate();

                    Integer count = settings.getMinmax();
                    if (count != null) {
                        int amount = countFluid(handler, extractMatcher);
                        int canextract = amount-count;
                        if (canextract <= 0) {
                            continue;
                        }
                        toextract = Math.min(toextract, canextract);
                    }

                    List<Pair<SidedConsumer, FluidConnectorSettings>> inserted = new ArrayList<>();
                    int remaining;
                    do {
                        // Imagine the pathological case where we're extracting from a container that works in 13mB
                        // increments and inserting into a container that works in 17mB increments. We should end up
                        // with toextract = 884 at the end of this loop, given that it started at 1000.
                        FluidStack stack = fetchFluid(handler, true, extractMatcher, toextract);
                        if (stack == null) continue extractorsLoop;
                        toextract = stack.amount;
                        inserted.clear();
                        remaining = insertFluidSimulate(inserted, context, stack);
                        toextract -= remaining;
                        if (inserted.isEmpty() || toextract <= 0) continue extractorsLoop;
                    } while(remaining > 0);
                    if (context.checkAndConsumeRF(ConfigSetup.controllerOperationRFT.get())) {
                        FluidStack stack = fetchFluid(handler, false, extractMatcher, toextract);
                        if (stack == null) {
                            throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.drain(" + toextract + ", true) returned null, even though handler.drain(" + toextract + ", false) did not");
                        }
                        insertFluidReal(context, inserted, stack);
                    }
                }
            }
        }

    }


    @Override
    public void cleanCache() {
        fluidExtractors = null;
        fluidConsumers = null;
    }

    private FluidStack fetchFluid(IFluidHandler handler, boolean simulate, @Nullable FluidStack matcher, int rate) {
        return handler.drain(rate, !simulate);
    }

    // Returns what could not be filled
    private int insertFluidSimulate(@Nonnull List<Pair<SidedConsumer, FluidConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull FluidStack stack) {
        World world = context.getControllerWorld();
        if (channelMode == ChannelMode.PRIORITY) {
            roundRobinOffset = 0;       // Always start at 0
        }
        int amount = stack.amount;
        for (int j = 0 ; j < fluidConsumers.size() ; j++) {
            int i = (j + roundRobinOffset)  % fluidConsumers.size();
            Pair<SidedConsumer, FluidConnectorSettings> entry = fluidConsumers.get(i);
            FluidConnectorSettings settings = entry.getValue();

            if (settings.getMatcher() == null || settings.getMatcher().equals(stack)) {
                BlockPos consumerPos = context.findConsumerPosition(entry.getKey().getConsumerId());
                if (consumerPos != null) {
                    if (!WorldTools.chunkLoaded(world, consumerPos)) {
                        continue;
                    }
                    if (checkRedstone(world, settings, consumerPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    EnumFacing side = entry.getKey().getSide();
                    BlockPos pos = consumerPos.offset(side);
                    TileEntity te = world.getTileEntity(pos);
                    IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing());
                    // @todo report error somewhere?
                    if (handler != null) {
                        int toinsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            int a = countFluid(handler, settings.getMatcher());
                            int caninsert = count-a;
                            if (caninsert <= 0) {
                                continue;
                            }
                            toinsert = Math.min(toinsert, caninsert);
                        }

                        FluidStack copy = stack.copy();
                        copy.amount = toinsert;

                        int filled = handler.fill(copy, false);
                        if (filled > 0) {
                            inserted.add(entry);
                            amount -= filled;
                            if (amount <= 0) {
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        return amount;
    }

    private int countFluid(IFluidHandler handler, @Nullable FluidStack matcher) {
        int cnt = 0;
        for (IFluidTankProperties properties : handler.getTankProperties()) {
            if (properties.getContents() != null && (matcher == null || matcher.equals(properties.getContents()))) {
                cnt += properties.getContents().amount;
            }
        }
        return cnt;
    }


    private void insertFluidReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, FluidConnectorSettings>> inserted, @Nonnull FluidStack stack) {
        int amount = stack.amount;
        for (Pair<SidedConsumer, FluidConnectorSettings> pair : inserted) {
            BlockPos consumerPosition = context.findConsumerPosition(pair.getKey().getConsumerId());
            EnumFacing side = pair.getKey().getSide();
            FluidConnectorSettings settings = pair.getValue();
            BlockPos pos = consumerPosition.offset(side);
            TileEntity te = context.getControllerWorld().getTileEntity(pos);
            IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing());

            int toinsert = Math.min(settings.getRate(), amount);

            Integer count = settings.getMinmax();
            if (count != null) {
                int a = countFluid(handler, settings.getMatcher());
                int caninsert = count-a;
                if (caninsert <= 0) {
                    continue;
                }
                toinsert = Math.min(toinsert, caninsert);
            }

            FluidStack copy = stack.copy();
            copy.amount = toinsert;

            int filled = handler.fill(copy, true);
            if (filled > 0) {
                roundRobinOffset = (roundRobinOffset+1) % fluidConsumers.size();
                amount -= filled;
                if (amount <= 0) {
                    return;
                }
            }
        }
    }



    private void updateCache(int channel, IControllerContext context) {
        if (fluidExtractors == null) {
            fluidExtractors = new HashMap<>();
            fluidConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                if (con.getFluidMode() == FluidConnectorSettings.FluidMode.EXT) {
                    fluidExtractors.put(entry.getKey(), con);
                } else {
                    fluidConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                if (con.getFluidMode() == FluidConnectorSettings.FluidMode.INS) {
                    fluidConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            fluidConsumers.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
        }
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 22, 80, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl().choices(TAG_MODE, "Fluid distribution mode", channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        channelMode = ChannelMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Nullable
    public static IFluidHandler getFluidHandlerAt(@Nullable TileEntity te, EnumFacing intSide) {
        if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, intSide)) {
            IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, intSide);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }
}

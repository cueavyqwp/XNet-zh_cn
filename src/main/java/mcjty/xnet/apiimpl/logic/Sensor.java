package mcjty.xnet.apiimpl.logic;

import mcjty.lib.varia.FluidTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.apiimpl.energy.EnergyChannelSettings;
import mcjty.xnet.apiimpl.fluids.FluidChannelSettings;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.compat.RFToolsSupport;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import static mcjty.rftoolsbase.api.xnet.channels.Color.COLORS;
import static mcjty.rftoolsbase.api.xnet.channels.Color.OFF;

public class Sensor {

    public static final String TAG_MODE = "mode";
    public static final String TAG_OPERATOR = "op";
    public static final String TAG_AMOUNT = "amount";
    public static final String TAG_COLOR = "scolor";
    public static final String TAG_STACK = "stack";


    public enum SensorMode {
        OFF,
        ITEM,
        FLUID,
        ENERGY,
        RS
    }

    public enum Operator {
        EQUAL("=", Integer::equals),
        NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
        LESS("<", (i1, i2) -> i1 < i2),
        GREATER(">", (i1, i2) -> i1 > i2),
        LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
        GREATOROREQUAL(">=", (i1, i2) -> i1 >= i2);

        private final String code;
        private final BiPredicate<Integer, Integer> matcher;

        private static final Map<String, Operator> OPERATOR_MAP = new HashMap<>();

        static {
            for (Operator operator : values()) {
                OPERATOR_MAP.put(operator.code, operator);
            }
        }

        Operator(String code, BiPredicate<Integer, Integer> matcher) {
            this.code = code;
            this.matcher = matcher;
        }

        public String getCode() {
            return code;
        }

        public boolean match(int i1, int i2) {
            return matcher.test(i1, i2);
        }

        @Override
        public String toString() {
            return code;
        }

        public static Operator valueOfCode(String code) {
            return OPERATOR_MAP.get(code);
        }
    }

    private final int index;

    private SensorMode sensorMode = SensorMode.OFF;
    private Operator operator = Operator.EQUAL;
    private int amount = 0;
    private Color outputColor = OFF;
    private ItemStack filter = ItemStack.EMPTY;

    public Sensor(int index) {
        this.index = index;
    }

    public SensorMode getSensorMode() {
        return sensorMode;
    }

    public void setSensorMode(SensorMode sensorMode) {
        this.sensorMode = sensorMode;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack filter) {
        this.filter = filter;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public void setOutputColor(Color outputColor) {
        this.outputColor = outputColor;
    }

    public boolean isEnabled(String tag) {
        if ((TAG_MODE + index).equals(tag)) {
            return true;
        }
        if ((TAG_OPERATOR + index).equals(tag)) {
            return true;
        }
        if ((TAG_AMOUNT + index).equals(tag)) {
            return true;
        }
        if ((TAG_COLOR + index).equals(tag)) {
            return true;
        }
        if ((TAG_STACK + index).equals(tag)) {
            return sensorMode == SensorMode.FLUID || sensorMode == SensorMode.ITEM;
        }
        return false;
    }

    public void createGui(IEditorGui gui) {
        gui
                .choices(TAG_MODE + index, "模式", sensorMode, SensorMode.values())
                .choices(TAG_OPERATOR + index, "运算符", operator, Operator.values())
                .integer(TAG_AMOUNT + index, "要比较的数据", amount, 46)
                .colors(TAG_COLOR + index, "输出颜色", outputColor.getColor(), COLORS)
                .ghostSlot(TAG_STACK + index, filter)
                .nl();
    }

    public boolean test(@Nullable TileEntity te, @Nonnull World world, @Nonnull BlockPos pos, LogicConnectorSettings settings) {
        switch (sensorMode) {
            case ITEM: {
                if (RFToolsSupport.isStorageScanner(te)) {
                    int cnt = RFToolsSupport.countItems(te, filter, amount + 1);
                    return operator.match(cnt, amount);
                } else {
                    return ItemChannelSettings.getItemHandlerAt(te, settings.getFacing()).map(h -> {
                        int cnt = countItem(h, filter, amount + 1);
                        return operator.match(cnt, amount);
                    }).orElse(false);
                }
            }
            case FLUID: {
                return FluidChannelSettings.getFluidHandlerAt(te, settings.getFacing()).map(h -> {
                    int cnt = countFluid(h, filter, amount + 1);
                    return operator.match(cnt, amount);
                }).orElse(false);
            }
            case ENERGY: {
                if (EnergyChannelSettings.isEnergyTE(te, settings.getFacing())) {
                    int cnt = EnergyChannelSettings.getEnergyLevel(te, settings.getFacing());
                    return operator.match(cnt, amount);
                }
                break;
            }
            case RS: {
                int cnt = world.getSignal(pos, settings.getFacing());
                return operator.match(cnt, amount);
            }
            case OFF:
            default:
                break;
        }

        return false;
    }

    private int safeInt(Object o) {
        if (o instanceof Integer) {
            return (Integer) o;
        } else {
            return 0;
        }
    }

    public void update(Map<String, Object> data) {
        Object sm = data.get(TAG_MODE + index);
        if (sm != null) {
            sensorMode = SensorMode.valueOf(((String) sm).toUpperCase());
        } else {
            sensorMode = SensorMode.OFF;
        }
        Object op = data.get(TAG_OPERATOR + index);
        if (op != null) {
            operator = Operator.valueOfCode(((String) op).toUpperCase());
        } else {
            operator = Operator.EQUAL;
        }
        amount = safeInt(data.get(TAG_AMOUNT + index));
        Object co = data.get(TAG_COLOR + index);
        if (co != null) {
            outputColor = Color.colorByValue((Integer) co);
        } else {
            outputColor = OFF;
        }
        filter = (ItemStack) data.get(TAG_STACK + index);
        if (filter == null) {
            filter = ItemStack.EMPTY;
        }
    }

    public void readFromNBT(CompoundNBT tag) {
        sensorMode = SensorMode.values()[tag.getByte("sensorMode" + index)];
        operator = Operator.values()[tag.getByte("operator" + index)];
        amount = tag.getInt("amount" + index);
        outputColor = Color.values()[tag.getByte("scolor" + index)];
        if (tag.contains("filter" + index)) {
            CompoundNBT itemTag = tag.getCompound("filter" + index);
            filter = ItemStack.of(itemTag);
        } else {
            filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundNBT tag) {
        tag.putByte("sensorMode" + index, (byte) sensorMode.ordinal());
        tag.putByte("operator" + index, (byte) operator.ordinal());
        tag.putInt("amount" + index, amount);
        tag.putByte("scolor" + index, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundNBT itemTag = new CompoundNBT();
            filter.save(itemTag);
            tag.put("filter" + index, itemTag);
        }
    }

    // Count items. We will stop early if we have enough to satisfy the sensor
    private int countItem(@Nonnull IItemHandler handler, ItemStack matcher, int maxNeeded) {
        int cnt = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!matcher.isEmpty()) {
                    // @todo 1.14 oredict?
                    if (matcher.sameItem(stack)) {
                        cnt += stack.getCount();
                        if (cnt >= maxNeeded) {
                            return cnt;
                        }
                    }
                } else {
                    cnt += stack.getCount();
                    if (cnt >= maxNeeded) {
                        return cnt;
                    }
                }
            }
        }
        return cnt;
    }

    private int countFluid(@Nonnull IFluidHandler handler, ItemStack matcher, int maxNeeded) {
        FluidStack fluidStack;
        if (!matcher.isEmpty()) {
            fluidStack = FluidTools.convertBucketToFluid(matcher);
        } else {
            fluidStack = null;
        }
        int cnt = 0;
        for (int i = 0 ; i < handler.getTanks() ; i++) {
            FluidStack contents = handler.getFluidInTank(i);
            if (!contents.isEmpty()) {
                if (fluidStack != null) {
                    if (fluidStack.isFluidEqual(contents)) {
                        cnt += contents.getAmount();
                        if (cnt >= maxNeeded) {
                            return cnt;
                        }
                    }
                } else {
                    cnt += contents.getAmount();
                    if (cnt >= maxNeeded) {
                        return cnt;
                    }
                }
            }
        }
        return cnt;
    }
}

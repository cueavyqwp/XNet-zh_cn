package mcjty.xnet.apiimpl.items;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.ItemStackList;
import mcjty.lib.varia.ItemStackTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.gui.IEditorGui;
import mcjty.xnet.api.gui.IndicatorIcon;
import mcjty.xnet.api.helper.AbstractConnectorSettings;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ItemConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";
    public static final String TAG_STACK = "stack";
    public static final String TAG_EXTRACT_AMOUNT = "extract_amount";
    public static final String TAG_SPEED = "speed";
    public static final String TAG_EXTRACT = "extract";
    public static final String TAG_OREDICT = "od";
    public static final String TAG_NBT = "nbt";
    public static final String TAG_META = "meta";
    public static final String TAG_PRIORITY = "priority";
    public static final String TAG_COUNT = "count";
    public static final String TAG_FILTER = "flt";
    public static final String TAG_BLACKLIST = "blacklist";

    public static final int FILTER_SIZE = 18;

    public enum ItemMode {
        INS,
        EXT
    }

    public enum StackMode {
        SINGLE,
        STACK,
        COUNT
    }

    public enum ExtractMode {
        FIRST,
        RND,
        ORDER
    }

    private ItemMode itemMode = ItemMode.INS;
    private ExtractMode extractMode = ExtractMode.FIRST;
    private int speed = 2;
    private StackMode stackMode = StackMode.SINGLE;
    private boolean oredictMode = false;
    private boolean metaMode = false;
    private boolean nbtMode = false;
    private boolean blacklist = false;
    @Nullable private Integer priority = 0;
    @Nullable private Integer count = null;
    @Nullable private Integer extractAmount = null;
    private ItemStackList filters = ItemStackList.create(FILTER_SIZE);

    // Cached matcher for items
    private Predicate<ItemStack> matcher = null;

    public ItemMode getItemMode() {
        return itemMode;
    }

    public ItemConnectorSettings(@Nonnull EnumFacing side) {
        super(side);
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (itemMode) {
            case INS:
                return new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case EXT:
                return new IndicatorIcon(iconGuiElements, 13, 70, 13, 10);
        }
        return null;
    }

    @Override
    @Nullable
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds;
        if (advanced) {
            speeds = new String[] { "5", "10", "20", "60", "100", "200" };
        } else {
            speeds = new String[] { "10", "20", "60", "100", "200" };
        }

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl()
                .choices(TAG_MODE, "Insert or extract mode", itemMode, ItemMode.values())
                .shift(5)
                .choices(TAG_STACK, "Single item, stack, or count", stackMode, StackMode.values());

        if (stackMode == StackMode.COUNT && itemMode == ItemMode.EXT) {
            gui
                    .integer(TAG_EXTRACT_AMOUNT, "Amount of items to extract|per operation", extractAmount, 30, 64);
        }

        gui
                .shift(10)
                .choices(TAG_SPEED, "Number of ticks for each operation", Integer.toString(speed * 5), speeds)
                .nl();

        gui
                .label("Pri").integer(TAG_PRIORITY, "Insertion priority", priority, 36).shift(5)
                .label("#")
                .integer(TAG_COUNT, itemMode == ItemMode.EXT ? "Amount in destination inventory|to keep" : "Max amount in destination|inventory", count, 30);

        if (itemMode == ItemMode.EXT) {
            gui
                    .shift(5)
                    .choices(TAG_EXTRACT, "Extract mode (first available,|random slot or round robin)", extractMode, ExtractMode.values());
        }

        gui
                .nl()

                .toggleText(TAG_BLACKLIST, "Enable blacklist mode", "BL", blacklist).shift(2)
                .toggleText(TAG_OREDICT, "Ore dictionary matching", "Ore", oredictMode).shift(2)
                .toggleText(TAG_META, "Metadata matching", "Meta", metaMode).shift(2)
                .toggleText(TAG_NBT, "NBT matching", "NBT", nbtMode)
                .nl();
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            gui.ghostSlot(TAG_FILTER + i, filters.get(i));
        }
    }

    public Predicate<ItemStack> getMatcher() {
        if (matcher == null) {
            ItemStackList filterList = ItemStackList.create();
            for (ItemStack stack : filters) {
                if (!stack.isEmpty()) {
                    filterList.add(stack);
                }
            }
            if (filterList.isEmpty()) {
                matcher = itemStack -> true;
            } else {
                ItemFilterCache filterCache = new ItemFilterCache(metaMode, oredictMode, blacklist, nbtMode, filterList);
                matcher = filterCache::match;
            }
        }
        return matcher;
    }

    public StackMode getStackMode() {
        return stackMode;
    }


    public ExtractMode getExtractMode() {
        return extractMode;
    }

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nullable
    public Integer getCount() {
        return count;
    }

    public int getExtractAmount() {
        return extractAmount == null ? 1 : extractAmount;
    }

    public int getSpeed() {
        return speed;
    }

    private static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_PRIORITY, TAG_OREDICT, TAG_META, TAG_NBT, TAG_BLACKLIST);
    private static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_OREDICT, TAG_META, TAG_NBT, TAG_BLACKLIST, TAG_STACK, TAG_SPEED, TAG_EXTRACT, TAG_EXTRACT_AMOUNT);

    @Override
    public boolean isEnabled(String tag) {
        if (tag.startsWith(TAG_FILTER)) {
            return true;
        }
        if (tag.equals(TAG_FACING)) {
            return advanced;
        }
        if (itemMode == ItemMode.INS) {
            return INSERT_TAGS.contains(tag);
        } else {
            return EXTRACT_TAGS.contains(tag);
        }
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        itemMode = ItemMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
        Object emode = data.get(TAG_EXTRACT);
        if (emode == null) {
            extractMode = ExtractMode.FIRST;
        } else {
            extractMode = ExtractMode.valueOf(((String) emode).toUpperCase());
        }
        stackMode = StackMode.valueOf(((String)data.get(TAG_STACK)).toUpperCase());
        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 5;
        if (speed == 0) {
            speed = 4;
        }
        oredictMode = Boolean.TRUE.equals(data.get(TAG_OREDICT));
        metaMode = Boolean.TRUE.equals(data.get(TAG_META));
        nbtMode = Boolean.TRUE.equals(data.get(TAG_NBT));

        blacklist = Boolean.TRUE.equals(data.get(TAG_BLACKLIST));
        priority = (Integer) data.get(TAG_PRIORITY);
        count = (Integer) data.get(TAG_COUNT);
        extractAmount = (Integer) data.get(TAG_EXTRACT_AMOUNT);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            filters.set(i, (ItemStack) data.get(TAG_FILTER+i));
        }
        matcher = null;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, "itemmode", itemMode);
        setEnumSafe(object, "extractmode", extractMode);
        setEnumSafe(object, "stackmode", stackMode);
        object.add("oredictmode", new JsonPrimitive(oredictMode));
        object.add("metamode", new JsonPrimitive(metaMode));
        object.add("nbtmode", new JsonPrimitive(nbtMode));
        object.add("blacklist", new JsonPrimitive(blacklist));
        setIntegerSafe(object, "priority", priority);
        setIntegerSafe(object, "extractamount", extractAmount);
        setIntegerSafe(object, "count", count);
        setIntegerSafe(object, "speed", speed);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (!filters.get(i).isEmpty()) {
                object.add("filter" + i, ItemStackTools.itemStackToJson(filters.get(i)));
            }
        }
        if (speed == 1) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }

        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        itemMode = getEnumSafe(object, "itemmode", EnumStringTranslators::getItemMode);
        extractMode = getEnumSafe(object, "extractmode", EnumStringTranslators::getExtractMode);
        stackMode = getEnumSafe(object, "stackmode", EnumStringTranslators::getStackMode);
        oredictMode = getBoolSafe(object, "oredictmode");
        metaMode = getBoolSafe(object, "metamode");
        nbtMode = getBoolSafe(object, "nbtmode");
        blacklist = getBoolSafe(object, "blacklist");
        priority = getIntegerSafe(object, "priority");
        extractAmount = getIntegerSafe(object, "extractamount");
        count = getIntegerSafe(object, "count");
        speed = getIntegerNotNull(object, "speed");
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (object.has("filter" + i)) {
                filters.set(i, ItemStackTools.jsonToItemStack(object.get("filter" + i).getAsJsonObject()));
            } else {
                filters.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        itemMode = ItemMode.values()[tag.getByte("itemMode")];
        extractMode = ExtractMode.values()[tag.getByte("extractMode")];
        stackMode = StackMode.values()[tag.getByte("stackMode")];
        if (tag.hasKey("spd")) {
            // New tag
            speed = tag.getInteger("spd");
        } else {
            // Old tag for compatibility
            speed = tag.getInteger("speed");
            if (speed == 0) {
                speed = 2;
            }
            speed *= 2;
        }
        oredictMode = tag.getBoolean("oredictMode");
        metaMode = tag.getBoolean("metaMode");
        nbtMode = tag.getBoolean("nbtMode");
        blacklist = tag.getBoolean("blacklist");
        if (tag.hasKey("priority")) {
            priority = tag.getInteger("priority");
        } else {
            priority = null;
        }
        if (tag.hasKey("extractAmount")) {
            extractAmount = tag.getInteger("extractAmount");
        } else {
            extractAmount = null;
        }
        if (tag.hasKey("count")) {
            count = tag.getInteger("count");
        } else {
            count = null;
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (tag.hasKey("filter" + i)) {
                NBTTagCompound itemTag = tag.getCompoundTag("filter" + i);
                filters.set(i, new ItemStack(itemTag));
            } else {
                filters.set(i, ItemStack.EMPTY);
            }
        }
        matcher = null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("itemMode", (byte) itemMode.ordinal());
        tag.setByte("extractMode", (byte) extractMode.ordinal());
        tag.setByte("stackMode", (byte) stackMode.ordinal());
        tag.setInteger("spd", speed);
        tag.setBoolean("oredictMode", oredictMode);
        tag.setBoolean("metaMode", metaMode);
        tag.setBoolean("nbtMode", nbtMode);
        tag.setBoolean("blacklist", blacklist);
        if (priority != null) {
            tag.setInteger("priority", priority);
        }
        if (extractAmount != null) {
            tag.setInteger("extractAmount", extractAmount);
        }
        if (count != null) {
            tag.setInteger("count", count);
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (!filters.get(i).isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                filters.get(i).writeToNBT(itemTag);
                tag.setTag("filter" + i, itemTag);
            }
        }
    }
}

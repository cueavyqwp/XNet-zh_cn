package mcjty.xnet.apiimpl.items;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.ItemStackList;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
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
    public static final String TAG_TAGS = "od";
    public static final String TAG_NBT = "nbt";
    public static final String TAG_META = "meta";
    public static final String TAG_PRIORITY = "priority";
    public static final String TAG_COUNT = "count";
    public static final String TAG_FILTER = "flt";
    public static final String TAG_FILTER_IDX = "fltIdx";
    public static final String TAG_BLACKLIST = "blacklist";

    public static final int FILTER_SIZE = 18;

    public enum ItemMode {
        输入,
        输出
    }

    public enum StackMode {
        单个物品,
        一组物品,
        指定数量
    }

    public enum ExtractMode {
        FIRST,
        RND,
        ORDER
    }

    private ItemMode itemMode = ItemMode.输入;
    private ExtractMode extractMode = ExtractMode.FIRST;
    private int speed = 2;
    private StackMode stackMode = StackMode.单个物品;
    private boolean tagsMode = false;
    private boolean metaMode = false;
    private boolean nbtMode = false;
    private boolean blacklist = false;
    @Nullable private Integer priority = 0;
    @Nullable private Integer count = null;
    @Nullable private Integer extractAmount = null;

    private ItemStackList filters = ItemStackList.create(FILTER_SIZE);
    private int filterIndex = -1;

    // Cached matcher for items
    private Predicate<ItemStack> matcher = null;

    public ItemMode getItemMode() {
        return itemMode;
    }

    public ItemConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (itemMode) {
            case 输入:
                return new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case 输出:
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
                .choices(TAG_MODE, "模式选择", itemMode, ItemMode.values())
                .shift(5)
                .choices(TAG_STACK, "单个物品 一组物品 或 指定数量", stackMode, StackMode.values());

        if (stackMode == StackMode.指定数量 && itemMode == ItemMode.输出) {
            gui
                    .integer(TAG_EXTRACT_AMOUNT, "每个操作要提取的项数", extractAmount, 30, 64);
        }

        gui
                .shift(10)
                .choices(TAG_SPEED, "每个操作所需的游戏刻", Integer.toString(speed * 5), speeds)
                .nl();

        gui
                .label("优先级").integer(TAG_PRIORITY, "优先级越高就越先处理", priority, 36).shift(5)
                .label("#")
                .integer(TAG_COUNT, itemMode == ItemMode.输出 ? "Amount in destination inventory|to keep" : "Max amount in destination|inventory", count, 30);

        if (itemMode == ItemMode.输出) {
            gui
                    .shift(5)
                    .choices(TAG_EXTRACT, "Extract mode (first available,|random slot or round robin)", extractMode, ExtractMode.values());
        }

        gui
                .nl()

                .toggleText(TAG_BLACKLIST, "启用黑名单", "黑名单", blacklist).shift(0)
                .toggleText(TAG_TAGS, "标签匹配", "标签", tagsMode).shift(0)
                .toggleText(TAG_META, "元数据匹配", "元数据", metaMode).shift(0)
                .toggleText(TAG_NBT, "NBT匹配", "NBT", nbtMode).shift(0)
                .choices(TAG_FILTER_IDX, "过滤索引", getFilterIndexString(), "关闭", "1", "2", "3", "4")
                .nl();
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            gui.ghostSlot(TAG_FILTER + i, filters.get(i));
        }
    }

    private String getFilterIndexString() {
        if (filterIndex == -1) {
            return "关闭";
        } else {
            return Integer.toString(filterIndex);
        }
    }

    public Predicate<ItemStack> getMatcher(IControllerContext context) {
        if (matcher == null) {
            ItemStackList filterList = ItemStackList.create();
            for (ItemStack stack : filters) {
                if (!stack.isEmpty()) {
                    filterList.add(stack);
                }
            }
            Predicate<ItemStack> filterMatcher = getIndexFilterMatcher(context);
            if (filterList.isEmpty()) {
                if (filterMatcher != null) {
                    matcher = filterMatcher;
                } else {
                    matcher = itemStack -> true;
                }
            } else {
                ItemFilterCache filterCache = new ItemFilterCache(metaMode, tagsMode, blacklist, nbtMode, filterList);
                if (filterMatcher != null) {
                    matcher = stack -> filterMatcher.test(stack) || filterCache.match(stack);
                } else {
                    matcher = filterCache::match;
                }
            }
        }
        return matcher;
    }

    @Nullable
    private Predicate<ItemStack> getIndexFilterMatcher(IControllerContext context) {
        if (filterIndex == -1) {
            return null;
        }
        return s -> context.getIndexedFilter(filterIndex-1).test(s);
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

    public int getFilterIndex() {
        return filterIndex;
    }

    private static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_PRIORITY, TAG_TAGS, TAG_META, TAG_NBT, TAG_BLACKLIST);
    private static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_TAGS, TAG_META, TAG_NBT, TAG_BLACKLIST, TAG_STACK, TAG_SPEED, TAG_EXTRACT, TAG_EXTRACT_AMOUNT);

    @Override
    public boolean isEnabled(String tag) {
        if (tag.startsWith(TAG_FILTER)) {
            return true;
        }
        if (tag.equals(TAG_FACING)) {
            return advanced;
        }
        if (itemMode == ItemMode.输入) {
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
        String idx = (String) data.get(TAG_FILTER_IDX);
        this.filterIndex = "关闭".equalsIgnoreCase(idx) ? -1 : Integer.parseInt(idx);
        tagsMode = Boolean.TRUE.equals(data.get(TAG_TAGS));
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
        object.add("tagsmode", new JsonPrimitive(tagsMode));
        object.add("metamode", new JsonPrimitive(metaMode));
        object.add("nbtmode", new JsonPrimitive(nbtMode));
        object.add("blacklist", new JsonPrimitive(blacklist));
        setIntegerSafe(object, "priority", priority);
        setIntegerSafe(object, "extractamount", extractAmount);
        setIntegerSafe(object, "count", count);
        setIntegerSafe(object, "speed", speed);
        setIntegerSafe(object, "filterindex", filterIndex);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (!filters.get(i).isEmpty()) {
                object.add("filter" + i, JSonTools.itemStackToJson(filters.get(i)));
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
        tagsMode = getBoolSafe(object, "tagsmode");
        metaMode = getBoolSafe(object, "metamode");
        nbtMode = getBoolSafe(object, "nbtmode");
        blacklist = getBoolSafe(object, "blacklist");
        priority = getIntegerSafe(object, "priority");
        extractAmount = getIntegerSafe(object, "extractamount");
        count = getIntegerSafe(object, "count");
        speed = getIntegerNotNull(object, "speed");
        if (object.has("filterindex")) {
            filterIndex = getIntegerNotNull(object, "filterindex");
        } else {
            filterIndex = -1;
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (object.has("filter" + i)) {
                filters.set(i, JSonTools.jsonToItemStack(object.get("filter" + i).getAsJsonObject()));
            } else {
                filters.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        itemMode = ItemMode.values()[tag.getByte("itemMode")];
        extractMode = ExtractMode.values()[tag.getByte("extractMode")];
        stackMode = StackMode.values()[tag.getByte("stackMode")];
        if (tag.contains("spd")) {
            // New tag
            speed = tag.getInt("spd");
        } else {
            // Old tag for compatibility
            speed = tag.getInt("speed");
            if (speed == 0) {
                speed = 2;
            }
            speed *= 2;
        }
        if (tag.contains("filterindex")) {
            filterIndex = tag.getInt("filterindex");
        } else {
            filterIndex = -1;
        }
        tagsMode = tag.getBoolean("tagsMode");
        metaMode = tag.getBoolean("metaMode");
        nbtMode = tag.getBoolean("nbtMode");
        blacklist = tag.getBoolean("blacklist");
        if (tag.contains("priority")) {
            priority = tag.getInt("priority");
        } else {
            priority = null;
        }
        if (tag.contains("extractAmount")) {
            extractAmount = tag.getInt("extractAmount");
        } else {
            extractAmount = null;
        }
        if (tag.contains("count")) {
            count = tag.getInt("count");
        } else {
            count = null;
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (tag.contains("filter" + i)) {
                CompoundNBT itemTag = tag.getCompound("filter" + i);
                filters.set(i, ItemStack.of(itemTag));
            } else {
                filters.set(i, ItemStack.EMPTY);
            }
        }
        matcher = null;
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putByte("itemMode", (byte) itemMode.ordinal());
        tag.putByte("extractMode", (byte) extractMode.ordinal());
        tag.putByte("stackMode", (byte) stackMode.ordinal());
        tag.putInt("spd", speed);
        tag.putInt("filterindex", filterIndex);
        tag.putBoolean("tagsMode", tagsMode);
        tag.putBoolean("metaMode", metaMode);
        tag.putBoolean("nbtMode", nbtMode);
        tag.putBoolean("blacklist", blacklist);
        if (priority != null) {
            tag.putInt("priority", priority);
        }
        if (extractAmount != null) {
            tag.putInt("extractAmount", extractAmount);
        }
        if (count != null) {
            tag.putInt("count", count);
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (!filters.get(i).isEmpty()) {
                CompoundNBT itemTag = new CompoundNBT();
                filters.get(i).save(itemTag);
                tag.put("filter" + i, itemTag);
            }
        }
    }
}

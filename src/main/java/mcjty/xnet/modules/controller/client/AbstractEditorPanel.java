package mcjty.xnet.modules.controller.client;

import mcjty.lib.blockcommands.Command;
import mcjty.lib.gui.events.BlockRenderEvent;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.rftoolsbase.api.xnet.channels.RSMode;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.XNet;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static mcjty.lib.gui.widgets.Widgets.textfield;

public abstract class AbstractEditorPanel implements IEditorGui {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final int LEFTMARGIN = 3;
    public static final int TOPMARGIN = 3;

    private final Panel panel;
    private final Minecraft mc;
    private final GuiController gui;
    protected final Map<String, Object> data;
    protected final Map<String, Widget<?>> components = new HashMap<>();

    private int x;
    private int y;

    protected abstract void update(String tag, Object value);

    public Widget<?> getComponent(String tag) {
        return components.get(tag);
    }

    protected void performUpdate(TypedMap.Builder builder, int i, Command<?> cmd) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object o = entry.getValue();
            if (o instanceof String) {
                builder.put(new Key<>(entry.getKey(), Type.STRING), (String) o);
            } else if (o instanceof Integer) {
                builder.put(new Key<>(entry.getKey(), Type.INTEGER), (Integer) o);
            } else if (o instanceof Boolean) {
                builder.put(new Key<>(entry.getKey(), Type.BOOLEAN), (Boolean) o);
            } else if (o instanceof Double) {
                builder.put(new Key<>(entry.getKey(), Type.DOUBLE), (Double) o);
            } else if (o instanceof ItemStack) {
                builder.put(new Key<>(entry.getKey(), Type.ITEMSTACK), (ItemStack) o);
            } else {
                builder.put(new Key<>(entry.getKey(), Type.STRING), o == null ? null : o.toString());
            }
        }

        gui.sendServerCommandTyped(XNetMessages.INSTANCE, cmd, builder.build());
        gui.refresh();
    }

    public AbstractEditorPanel(Panel panel, Minecraft mc, GuiController gui) {
        this.panel = panel;
        this.mc = mc;
        this.gui = gui;
        x = LEFTMARGIN;
        y = TOPMARGIN;
        data = new HashMap<>();
    }

    @Override
    public IEditorGui move(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public IEditorGui move(int x) {
        this.x = x;
        return this;
    }

    @Override
    public IEditorGui shift(int x) {
        this.x += x;
        return this;
    }

    private void fitWidth(int w) {
        if (x + w > panel.getBounds().width) {
            nl();
        }
    }

    private String[] parseTooltips(String tooltip) {
        return StringUtils.split(tooltip, '|');
    }

    @Override
    public IEditorGui label(String txt) {
        int w = mc.font.width(txt)+5;
        fitWidth(w);
        Label label = Widgets.label(x, y, w, 14, txt);
        panel.children(label);
        x += w;
        return this;
    }

    @Override
    public IEditorGui text(String tag, String tooltip, String value, int width) {
        fitWidth(width);
        TextField text = textfield(x, y, width, 14).text(value)
                .tooltips(parseTooltips(tooltip));
        data.put(tag, value);
        text.addTextEnterEvent((newText) -> update(tag, newText));
        text.event((newText) -> update(tag, newText));
        panel.children(text);
        components.put(tag, text);
        x += width;
        return this;
    }

    private Integer parseInt(String i, Integer maximum) {
        if (i == null || i.isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(i);
            if (maximum != null && v > maximum) {
                v = maximum;
            }
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IEditorGui integer(String tag, String tooltip, Integer value, int width) {
        return integer(tag, tooltip, value, width, null);
    }

    @Override
    public IEditorGui integer(String tag, String tooltip, Integer value, int width, Integer maximum) {
        fitWidth(width);
        TextField text = textfield(x, y, width, 14).text(value == null ? "" : value.toString())
                .tooltips(parseTooltips(tooltip));
        data.put(tag, value);
        text.addTextEnterEvent((newText) -> update(tag, parseInt(newText, maximum)));
        text.event((newText) -> update(tag, parseInt(newText, maximum)));
        panel.children(text);
        components.put(tag, text);
        x += width;
        return this;
    }

    private Double parseDouble(String i) {
        if (i == null || i.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(i);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IEditorGui real(String tag, String tooltip, Double value, int width) {
        fitWidth(width);
        TextField text = textfield(x, y, width, 14).text(value == null ? "" : value.toString())
                .tooltips(parseTooltips(tooltip));
        data.put(tag, value);
        text.addTextEnterEvent((newText) -> update(tag, parseDouble(newText)));
        text.event((newText) -> update(tag, parseDouble(newText)));
        panel.children(text);
        components.put(tag, text);
        x += width;
        return this;
    }

    @Override
    public IEditorGui toggle(String tag, String tooltip, boolean value) {
        int w = 12;
        fitWidth(w);
        ToggleButton toggle = new ToggleButton().checkMarker(true).pressed(value)
                .tooltips(parseTooltips(tooltip))
                .hint(x, y, w, 14);
        data.put(tag, value);
        toggle.event(() -> update(tag, toggle.isPressed()));
        panel.children(toggle);
        components.put(tag, toggle);
        x += w;
        return this;
    }

    @Override
    public IEditorGui toggleText(String tag, String tooltip, String text, boolean value) {
        int w = mc.font.width(text) + 10;
        fitWidth(w);
        ToggleButton toggle = new ToggleButton().checkMarker(false).pressed(value)
                .text(text)
                .tooltips(parseTooltips(tooltip))
                .hint(x, y, w, 14);
        data.put(tag, value);
        toggle.event(() -> update(tag, toggle.isPressed()));
        panel.children(toggle);
        components.put(tag, toggle);
        x += w;
        return this;
    }

    @Override
    public IEditorGui colors(String tag, String tooltip, Integer current, Integer... colors) {
        int w = 14;
        fitWidth(w);
        ColorChoiceLabel choice = new ColorChoiceLabel().colors(colors).currentColor(current)
                .tooltips(parseTooltips(tooltip))
                .hint(x, y, w, 14);
        data.put(tag, current);
        choice.event((newChoice) -> update(tag, newChoice));
        panel.children(choice);
        components.put(tag, choice);
        x += w;
        return this;
    }

    @Override
    public IEditorGui choices(String tag, String tooltip, String current, String... values) {
        int w = 10;
        for (String s : values) {
            w = Math.max(w, mc.font.width(s) + 14);
        }

        fitWidth(w);
        ChoiceLabel choice = new ChoiceLabel().choices(values).choice(current)
                .tooltips(parseTooltips(tooltip))
                .hint(x, y, w, 14);
        data.put(tag, current);
        choice.event((newChoice) -> update(tag, newChoice));
        panel.children(choice);
        components.put(tag, choice);
        x += w;
        return this;
    }

    @Override
    public <T extends Enum<T>> IEditorGui choices(String tag, String tooltip, T current, T... values) {
        String[] strings = new String[values.length];
        int i = 0;
        for (T s : values) {
            strings[i++] = StringUtils.capitalize(s.toString().toLowerCase());
        }
        return choices(tag, tooltip, StringUtils.capitalize(current.toString().toLowerCase()), strings);
    }

    @Override
    public IEditorGui redstoneMode(String tag, RSMode current) {
        int w = 14;
        fitWidth(w);
        ImageChoiceLabel redstoneMode = new ImageChoiceLabel()
                .choice("IGNORED", "忽略红石信号", iconGuiElements, 1, 1)
                .choice("OFF", "关闭红石信号以激活", iconGuiElements, 17, 1)
                .choice("ON", "开启红石信号以激活", iconGuiElements, 33, 1)
                .choice("PULSE", "一次脉冲激活一次", iconGuiElements, 49, 1);
        switch (current) {
            case IGNORED:
                redstoneMode.setCurrentChoice("IGNORED");
                break;
            case OFF:
                redstoneMode.setCurrentChoice("OFF");
                break;
            case ON:
                redstoneMode.setCurrentChoice("ON");
                break;
            case PULSE:
                redstoneMode.setCurrentChoice("PULSE");
                break;
        }
        redstoneMode.hint(x, y, w, 14);
        data.put(tag, current.name());
        redstoneMode.event((newChoice) -> update(tag, newChoice));
        panel.children(redstoneMode);
        components.put(tag, redstoneMode);
        x += w;
        return this;
    }

    @Override
    public IEditorGui ghostSlot(String tag, ItemStack stack) {
        int w = 16;
        fitWidth(w);
        BlockRender blockRender = new BlockRender()
                .renderItem(stack)
                .desiredWidth(18).desiredHeight(18)
                .filledRectThickness(-1).filledBackground(0xff888888);
        blockRender.event(new BlockRenderEvent() {
            @Override
            public void select() {
                //noinspection ConstantConditions
                ItemStack holding = Minecraft.getInstance().player.inventory.getCarried();
                if (holding.isEmpty()) {
                    update(tag, holding);
                    blockRender.renderItem(null);
                } else {
                    ItemStack copy = holding.copy();
                    copy.setCount(1);
                    blockRender.renderItem(copy);
                    update(tag, copy);
                }
            }

            @Override
            public void doubleClick() {

            }
        });
        blockRender.hint(x, y-1, 17, 17);
        data.put(tag, stack);
        panel.children(blockRender);
        components.put(tag, blockRender);
        x += w;
        return this;
    }

    @Override
    public IEditorGui nl() {
        y += 16;
        x = LEFTMARGIN;
        return this;
    }
}

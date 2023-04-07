package mcjty.xnet.blocks.generic;

import net.minecraft.util.IStringSerializable;

public enum CableColor implements IStringSerializable {
    BLUE("blue", "dyeBlue"),
    RED("red", "dyeRed"),
    YELLOW("yellow", "dyeYellow"),
    GREEN("green", "dyeGreen"),
    ROUTING("routing", null);

    public static final CableColor[] VALUES = CableColor.values();

    private final String name;
    private final String dye;

    CableColor(String name, String dye) {
        this.name = name; this.dye = dye;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDye() {
        return dye;
    }
}

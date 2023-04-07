package mcjty.xnet.blocks.facade;

import net.minecraftforge.common.property.IUnlistedProperty;

public class FacadeProperty implements IUnlistedProperty<FacadeBlockId> {

    private final String name;

    public FacadeProperty(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(FacadeBlockId value) {
        return true;
    }

    @Override
    public Class<FacadeBlockId> getType() {
        return FacadeBlockId.class;
    }

    @Override
    public String valueToString(FacadeBlockId value) {
        return value.toString();
    }
}

package mcjty.xnet.blocks.generic;

import mcjty.xnet.blocks.cables.ConnectorType;
import net.minecraftforge.common.property.IUnlistedProperty;

public class UnlistedPropertyBlockType implements IUnlistedProperty<ConnectorType> {

    private final String name;

    public UnlistedPropertyBlockType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(ConnectorType value) {
        return true;
    }

    @Override
    public Class<ConnectorType> getType() {
        return ConnectorType.class;
    }

    @Override
    public String valueToString(ConnectorType value) {
        return value.toString();
    }
}

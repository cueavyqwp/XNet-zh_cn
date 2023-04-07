package mcjty.xnet.blocks.cables;

import mcjty.xnet.config.ConfigSetup;

public class AdvancedConnectorTileEntity extends ConnectorTileEntity {

    @Override
    public int getMaxEnergy() {
        return ConfigSetup.maxRfAdvancedConnector.get();
    }
}

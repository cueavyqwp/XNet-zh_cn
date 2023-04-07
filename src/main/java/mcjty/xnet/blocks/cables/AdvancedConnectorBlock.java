package mcjty.xnet.blocks.cables;

import mcjty.xnet.XNet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class AdvancedConnectorBlock extends ConnectorBlock {

    public static final String ADVANCED_CONNECTOR = "advanced_connector";

    public AdvancedConnectorBlock() {
        super(ADVANCED_CONNECTOR);
    }

    @Override
    protected void initTileEntity() {
        GameRegistry.registerTileEntity(AdvancedConnectorTileEntity.class, XNet.MODID + ":advanced_connector");
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState metadata) {
        return new AdvancedConnectorTileEntity();
    }


    @Override
    public boolean isAdvancedConnector() {
        return true;
    }
}

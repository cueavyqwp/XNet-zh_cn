package mcjty.xnet.blocks.cables;

import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class NetCableSetup {
    public static NetCableBlock netCableBlock;
    public static ConnectorBlock connectorBlock;
    public static AdvancedConnectorBlock advancedConnectorBlock;

    public static void init() {
        netCableBlock = new NetCableBlock();
        connectorBlock = new ConnectorBlock();
        advancedConnectorBlock = new AdvancedConnectorBlock();
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        netCableBlock.initModel();
        connectorBlock.initModel();
        advancedConnectorBlock.initModel();
    }

    @SideOnly(Side.CLIENT)
    public static void initItemModels() {
        netCableBlock.initItemModel();
        connectorBlock.initItemModel();
        advancedConnectorBlock.initItemModel();
    }

    @SideOnly(Side.CLIENT)
    public static void initColorHandlers(BlockColors blockColors) {
        connectorBlock.initColorHandler(blockColors);
        advancedConnectorBlock.initColorHandler(blockColors);
    }

    public static void initCrafting() {


        // @todo recipes
//        for (CableColor source : CableColor.VALUES) {
//            if (source != CableColor.ROUTING) {
//                for (CableColor dest : CableColor.VALUES) {
//                    if (dest != source && dest != CableColor.ROUTING) {
//                        MyGameReg.addRecipe(new ItemStack(netCableBlock, 1, dest.ordinal()), new ItemStack(netCableBlock, 1, source.ordinal()), dest.getDye());
//                        MyGameReg.addRecipe(new ItemStack(connectorBlock, 1, dest.ordinal()), new ItemStack(connectorBlock, 1, source.ordinal()), dest.getDye());
//                        MyGameReg.addRecipe(new ItemStack(advancedConnectorBlock, 1, dest.ordinal()), new ItemStack(advancedConnectorBlock, 1, source.ordinal()), dest.getDye());
//                    }
//                }
//            }
//        }
    }
}

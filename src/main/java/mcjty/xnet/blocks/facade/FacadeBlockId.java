package mcjty.xnet.blocks.facade;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class FacadeBlockId {
    private final String registryName;
    private final int meta;

    public FacadeBlockId(IBlockState mimicBlock) {
        Block block = mimicBlock.getBlock();
        this.registryName = block.getRegistryName().toString();
        this.meta = block.getMetaFromState(mimicBlock);
    }

    public IBlockState getBlockState() {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registryName)).getStateFromMeta(meta);
    }

    @Override
    public String toString() {
        return registryName + '@' + meta;
    }
}

package mcjty.xnet.blocks.generic;

import mcjty.lib.blocks.GenericBlock;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.xnet.XNet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class GenericXNetBlock<T extends GenericTileEntity, C extends Container> extends GenericBlock<T, C> {

    public GenericXNetBlock(Material material, Class<? extends T> tileEntityClass,
                            BiFunction<EntityPlayer, IInventory, C> containerFactory,
                            Function<Block, ItemBlock> itemBlockFunction, String name, boolean isContainer) {
        super(XNet.instance, material, tileEntityClass, containerFactory, itemBlockFunction, name, isContainer);
        setCreativeTab(XNet.setup.getTab());
    }
}

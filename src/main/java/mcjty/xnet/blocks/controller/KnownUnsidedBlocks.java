package mcjty.xnet.blocks.controller;

import mcjty.xnet.config.ConfigSetup;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class KnownUnsidedBlocks {

    private static final Set<ResourceLocation> UNSIDED_BLOCKS = new HashSet<>();

    public static boolean isUnsided(ResourceLocation resourceLocation) {
        if (UNSIDED_BLOCKS.isEmpty()) {
            for (String block : ConfigSetup.unsidedBlocks.get()) {
                UNSIDED_BLOCKS.add(new ResourceLocation(block));
            }
        }
        return UNSIDED_BLOCKS.contains(resourceLocation);
    }

}

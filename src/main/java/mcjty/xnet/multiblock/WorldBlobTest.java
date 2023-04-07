package mcjty.xnet.multiblock;

import mcjty.xnet.api.keys.NetworkId;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class WorldBlobTest {

    private static void createCableLine(WorldBlob world, BlockPos p1, EnumFacing direction, int count, ColorId color) {
        for (int i = 0 ; i < count ; i++) {
            world.createCableSegment(p1, color);
            p1 = p1.offset(direction);
        }
    }


    public static void main(String[] args) {
        ColorId color1 = new ColorId(111);
        ColorId color2 = new ColorId(222);
        ColorId color3 = new ColorId(333);

        WorldBlob world = new WorldBlob(0);

        BlockPos p1 = new BlockPos(10, 60, 10);
        world.createNetworkProvider(p1, color1, new NetworkId(1000));
        createCableLine(world, p1.east(), EnumFacing.EAST, 20, color1);

        BlockPos p2 = new BlockPos(50, 61, 10);
        world.createNetworkProvider(p2, color2, new NetworkId(2000));
        createCableLine(world, p2.west(), EnumFacing.WEST, 50, color2);

        BlockPos p3 = new BlockPos(50, 30, 1000);
        createCableLine(world, p3, EnumFacing.UP, 5, color3);
        createCableLine(world, p3.east().east(), EnumFacing.UP, 5, color3);
        world.createNetworkProvider(p3.north(), color3, new NetworkId(3000));
        world.createCableSegment(p3.east(), color3);

        world.dump();

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");

        world.removeCableSegment(new BlockPos(30, 61, 10));

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        world.dump();

        NBTTagCompound compound = new NBTTagCompound();
        world.writeToNBT(compound);

        world = new WorldBlob(0);
        world.readFromNBT(compound);

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        world.dump();
    }
}

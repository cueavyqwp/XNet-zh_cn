package mcjty.xnet;

import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.cables.ConnectorType;
import mcjty.xnet.blocks.facade.FacadeBlock;
import mcjty.xnet.blocks.facade.FacadeBlockId;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.blocks.generic.GenericCableBlock;
import mcjty.xnet.config.ConfigSetup;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static mcjty.xnet.blocks.cables.ConnectorType.BLOCK;
import static mcjty.xnet.blocks.cables.ConnectorType.CABLE;

@SideOnly(Side.CLIENT)
public class RenderWorldLastEventHandler {

    private static long lastTime = 0;

    public static void tick(RenderWorldLastEvent evt) {
        renderHilightedBlock(evt);
        renderCables(evt);
    }

    private static void renderCables(RenderWorldLastEvent evt) {
        Minecraft mc = Minecraft.getMinecraft();

        EntityPlayerSP p = mc.player;

        ItemStack heldItem = p.getHeldItem(EnumHand.MAIN_HAND);
        if (!heldItem.isEmpty()) {
            if (heldItem.getItem() instanceof ItemBlock) {
                if (((ItemBlock) heldItem.getItem()).getBlock() instanceof GenericCableBlock) {
                    renderCablesInt(evt, mc);
                }
            }
        }
    }

    private static void renderCablesInt(RenderWorldLastEvent evt, Minecraft mc) {
        EntityPlayerSP p = mc.player;
        WorldClient world = mc.world;
        double doubleX = p.lastTickPosX + (p.posX - p.lastTickPosX) * evt.getPartialTicks();
        double doubleY = p.lastTickPosY + (p.posY - p.lastTickPosY) * evt.getPartialTicks();
        double doubleZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * evt.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 0, 0);
        GlStateManager.glLineWidth(2);
        GlStateManager.translate(-doubleX, -doubleY, -doubleZ);

        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (int dx = -20 ; dx <= 20 ; dx++) {
            for (int dy = -20 ; dy <= 20 ; dy++) {
                for (int dz = -20 ; dz <= 20 ; dz++) {
                    BlockPos c = p.getPosition().add(dx, dy, dz);
                    IBlockState state = world.getBlockState(c);
                    Block block = state.getBlock();
                    if (block instanceof FacadeBlock || block instanceof ConnectorBlock || block instanceof GenericCableBlock) {
                        IExtendedBlockState extendedBlockState;
                        if (state.getBlock() instanceof FacadeBlock) {
                            extendedBlockState = (IExtendedBlockState) ((FacadeBlock) state.getBlock()).getStateInternal(state, world, c);
                        } else {
                            extendedBlockState = (IExtendedBlockState) state.getBlock().getExtendedState(state, world, c);
                        }
                        FacadeBlockId facadeId = extendedBlockState.getValue(GenericCableBlock.FACADEID);
                        if (((!ConfigSetup.showNonFacadedCablesWhileSneaking.get()) || (!p.isSneaking())) && facadeId == null && !(block instanceof FacadeBlock)) {
                            continue;
                        }
                        CableColor color = extendedBlockState.getValue(GenericCableBlock.COLOR);
                        float r = 0;
                        float g = 0;
                        float b = 0;
                        switch (color) {
                            case BLUE:
                                r = .4f;
                                g = .4f;
                                b = 1f;
                                break;
                            case RED:
                                r = 1f;
                                g = .4f;
                                b = .4f;
                                break;
                            case YELLOW:
                                r = 1f;
                                g = 1f;
                                b = .4f;
                                break;
                            case GREEN:
                                r = .4f;
                                g = 1f;
                                b = .4f;
                                break;
                            case ROUTING:
                                r = .7f;
                                g = .7f;
                                b = .7f;
                                break;
                        }
                        List<Rect> quads = getQuads(extendedBlockState);
                        for (Rect quad : quads) {
                            renderRect(buffer, quad, c, r, g, b, 0.5f);
                        }
                    }
                }
            }
        }

        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }


    private static Vec3d v(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    private static class Rect {
        public Vec3d v1;
        public Vec3d v2;
        public Vec3d v3;
        public Vec3d v4;

        public Rect(Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
        }
    }

    private static List<Rect> getQuads(IExtendedBlockState extendedBlockState) {
        ConnectorType north = extendedBlockState.getValue(GenericCableBlock.NORTH);
        ConnectorType south = extendedBlockState.getValue(GenericCableBlock.SOUTH);
        ConnectorType west = extendedBlockState.getValue(GenericCableBlock.WEST);
        ConnectorType east = extendedBlockState.getValue(GenericCableBlock.EAST);
        ConnectorType up = extendedBlockState.getValue(GenericCableBlock.UP);
        ConnectorType down = extendedBlockState.getValue(GenericCableBlock.DOWN);
        List<Rect> quads = new ArrayList<>();

        double o = .4;      // Thickness of the cable. .0 would be full block, .5 is infinitely thin.
        double p = .1;      // Thickness of the connector as it is put on the connecting block
        double q = .2;      // The wideness of the connector

        // For each side we either cap it off if there is no similar block adjacent on that side
        // or else we extend so that we touch the adjacent block:

        if (up == CABLE) {
            quads.add(new Rect(v(1 - o, 1, o), v(1 - o, 1, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o)));
            quads.add(new Rect(v(o, 1, 1 - o), v(o, 1, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, 1, o), v(1 - o, 1, o), v(1 - o, 1 - o, o), v(o, 1 - o, o)));
            quads.add(new Rect(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1, 1 - o), v(o, 1, 1 - o)));
        } else if (up == BLOCK) {
            quads.add(new Rect(v(1 - o, 1 - p, o), v(1 - o, 1 - p, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o)));
            quads.add(new Rect(v(o, 1 - p, 1 - o), v(o, 1 - p, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, 1 - p, o), v(1 - o, 1 - p, o), v(1 - o, 1 - o, o), v(o, 1 - o, o)));
            quads.add(new Rect(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - p, 1 - o), v(o, 1 - p, 1 - o)));

            quads.add(new Rect(v(1 - q, 1 - p, q), v(1 - q, 1, q), v(1 - q, 1, 1 - q), v(1 - q, 1 - p, 1 - q)));
            quads.add(new Rect(v(q, 1 - p, 1 - q), v(q, 1, 1 - q), v(q, 1, q), v(q, 1 - p, q)));
            quads.add(new Rect(v(q, 1, q), v(1 - q, 1, q), v(1 - q, 1 - p, q), v(q, 1 - p, q)));
            quads.add(new Rect(v(q, 1 - p, 1 - q), v(1 - q, 1 - p, 1 - q), v(1 - q, 1, 1 - q), v(q, 1, 1 - q)));

            quads.add(new Rect(v(q, 1 - p, q), v(1 - q, 1 - p, q), v(1 - q, 1 - p, 1 - q), v(q, 1 - p, 1 - q)));
            quads.add(new Rect(v(q, 1, q), v(q, 1, 1 - q), v(1 - q, 1, 1 - q), v(1 - q, 1, q)));
        } else {
            quads.add(new Rect(v(o,     1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o),     v(o,     1 - o, o)));
        }

        if (down == CABLE) {
            quads.add(new Rect(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, 0, o)));
            quads.add(new Rect(v(o, o, 1 - o), v(o, o, o), v(o, 0, o), v(o, 0, 1 - o)));
            quads.add(new Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, 0, o), v(o, 0, o)));
            quads.add(new Rect(v(o, 0, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));
        } else if (down == BLOCK) {
            quads.add(new Rect(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, p, 1 - o), v(1 - o, p, o)));
            quads.add(new Rect(v(o, o, 1 - o), v(o, o, o), v(o, p, o), v(o, p, 1 - o)));
            quads.add(new Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, p, o), v(o, p, o)));
            quads.add(new Rect(v(o, p, 1 - o), v(1 - o, p, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));

            quads.add(new Rect(v(1 - q, 0, q), v(1 - q, p, q), v(1 - q, p, 1 - q), v(1 - q, 0, 1 - q)));
            quads.add(new Rect(v(q, 0, 1 - q), v(q, p, 1 - q), v(q, p, q), v(q, 0, q)));
            quads.add(new Rect(v(q, p, q), v(1 - q, p, q), v(1 - q, 0, q), v(q, 0, q)));
            quads.add(new Rect(v(q, 0, 1 - q), v(1 - q, 0, 1 - q), v(1 - q, p, 1 - q), v(q, p, 1 - q)));

            quads.add(new Rect(v(q, p, 1 - q), v(1 - q, p, 1 - q), v(1 - q, p, q), v(q, p, q)));
            quads.add(new Rect(v(q, 0, 1 - q), v(q, 0, q), v(1 - q, 0, q), v(1 - q, 0, 1 - q)));
        } else {
            quads.add(new Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));
        }

        if (east == CABLE) {
            quads.add(new Rect(v(1, 1 - o, 1 - o), v(1, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o)));
            quads.add(new Rect(v(1, o, o), v(1, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o)));
            quads.add(new Rect(v(1, 1 - o, o), v(1, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o)));
            quads.add(new Rect(v(1, o, 1 - o), v(1, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));
        } else if (east == BLOCK) {
            quads.add(new Rect(v(1 - p, 1 - o, 1 - o), v(1 - p, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o)));
            quads.add(new Rect(v(1 - p, o, o), v(1 - p, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o)));
            quads.add(new Rect(v(1 - p, 1 - o, o), v(1 - p, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o)));
            quads.add(new Rect(v(1 - p, o, 1 - o), v(1 - p, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));

            quads.add(new Rect(v(1 - p, 1 - q, 1 - q), v(1, 1 - q, 1 - q), v(1, 1 - q, q), v(1 - p, 1 - q, q)));
            quads.add(new Rect(v(1 - p, q, q), v(1, q, q), v(1, q, 1 - q), v(1 - p, q, 1 - q)));
            quads.add(new Rect(v(1 - p, 1 - q, q), v(1, 1 - q, q), v(1, q, q), v(1 - p, q, q)));
            quads.add(new Rect(v(1 - p, q, 1 - q), v(1, q, 1 - q), v(1, 1 - q, 1 - q), v(1 - p, 1 - q, 1 - q)));

            quads.add(new Rect(v(1 - p, q, 1 - q), v(1 - p, 1 - q, 1 - q), v(1 - p, 1 - q, q), v(1 - p, q, q)));
            quads.add(new Rect(v(1, q, 1 - q), v(1, q, q), v(1, 1 - q, q), v(1, 1 - q, 1 - q)));
        } else {
            quads.add(new Rect(v(1 - o, o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));
        }

        if (west == CABLE) {
            quads.add(new Rect(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(0, 1 - o, o), v(0, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, o, o), v(o, o, 1 - o), v(0, o, 1 - o), v(0, o, o)));
            quads.add(new Rect(v(o, 1 - o, o), v(o, o, o), v(0, o, o), v(0, 1 - o, o)));
            quads.add(new Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(0, 1 - o, 1 - o), v(0, o, 1 - o)));
        } else if (west == BLOCK) {
            quads.add(new Rect(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(p, 1 - o, o), v(p, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, o, o), v(o, o, 1 - o), v(p, o, 1 - o), v(p, o, o)));
            quads.add(new Rect(v(o, 1 - o, o), v(o, o, o), v(p, o, o), v(p, 1 - o, o)));
            quads.add(new Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(p, 1 - o, 1 - o), v(p, o, 1 - o)));

            quads.add(new Rect(v(0, 1 - q, 1 - q), v(p, 1 - q, 1 - q), v(p, 1 - q, q), v(0, 1 - q, q)));
            quads.add(new Rect(v(0, q, q), v(p, q, q), v(p, q, 1 - q), v(0, q, 1 - q)));
            quads.add(new Rect(v(0, 1 - q, q), v(p, 1 - q, q), v(p, q, q), v(0, q, q)));
            quads.add(new Rect(v(0, q, 1 - q), v(p, q, 1 - q), v(p, 1 - q, 1 - q), v(0, 1 - q, 1 - q)));

            quads.add(new Rect(v(p, q, q), v(p, 1 - q, q), v(p, 1 - q, 1 - q), v(p, q, 1 - q)));
            quads.add(new Rect(v(0, q, q), v(0, q, 1 - q), v(0, 1 - q, 1 - q), v(0, 1 - q, q)));
        } else {
            quads.add(new Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(o, o, o)));
        }

        if (north == CABLE) {
            quads.add(new Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 0), v(o, 1 - o, 0)));
            quads.add(new Rect(v(o, o, 0), v(1 - o, o, 0), v(1 - o, o, o), v(o, o, o)));
            quads.add(new Rect(v(1 - o, o, 0), v(1 - o, 1 - o, 0), v(1 - o, 1 - o, o), v(1 - o, o, o)));
            quads.add(new Rect(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, 0), v(o, o, 0)));
        } else if (north == BLOCK) {
            quads.add(new Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, p), v(o, 1 - o, p)));
            quads.add(new Rect(v(o, o, p), v(1 - o, o, p), v(1 - o, o, o), v(o, o, o)));
            quads.add(new Rect(v(1 - o, o, p), v(1 - o, 1 - o, p), v(1 - o, 1 - o, o), v(1 - o, o, o)));
            quads.add(new Rect(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, p), v(o, o, p)));

            quads.add(new Rect(v(q, 1 - q, p), v(1 - q, 1 - q, p), v(1 - q, 1 - q, 0), v(q, 1 - q, 0)));
            quads.add(new Rect(v(q, q, 0), v(1 - q, q, 0), v(1 - q, q, p), v(q, q, p)));
            quads.add(new Rect(v(1 - q, q, 0), v(1 - q, 1 - q, 0), v(1 - q, 1 - q, p), v(1 - q, q, p)));
            quads.add(new Rect(v(q, q, p), v(q, 1 - q, p), v(q, 1 - q, 0), v(q, q, 0)));

            quads.add(new Rect(v(q, q, p), v(1 - q, q, p), v(1 - q, 1 - q, p), v(q, 1 - q, p)));
            quads.add(new Rect(v(q, q, 0), v(q, 1 - q, 0), v(1 - q, 1 - q, 0), v(1 - q, q, 0)));
        } else {
            quads.add(new Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, o, o), v(o, o, o)));
        }

        if (south == CABLE) {
            quads.add(new Rect(v(o, 1 - o, 1), v(1 - o, 1 - o, 1), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1), v(o, o, 1)));
            quads.add(new Rect(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1), v(1 - o, o, 1)));
            quads.add(new Rect(v(o, o, 1), v(o, 1 - o, 1), v(o, 1 - o, 1 - o), v(o, o, 1 - o)));
        } else if (south == BLOCK) {
            quads.add(new Rect(v(o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
            quads.add(new Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1 - p), v(o, o, 1 - p)));
            quads.add(new Rect(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - p), v(1 - o, o, 1 - p)));
            quads.add(new Rect(v(o, o, 1 - p), v(o, 1 - o, 1 - p), v(o, 1 - o, 1 - o), v(o, o, 1 - o)));

            quads.add(new Rect(v(q, 1 - q, 1), v(1 - q, 1 - q, 1), v(1 - q, 1 - q, 1 - p), v(q, 1 - q, 1 - p)));
            quads.add(new Rect(v(q, q, 1 - p), v(1 - q, q, 1 - p), v(1 - q, q, 1), v(q, q, 1)));
            quads.add(new Rect(v(1 - q, q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, 1 - q, 1), v(1 - q, q, 1)));
            quads.add(new Rect(v(q, q, 1), v(q, 1 - q, 1), v(q, 1 - q, 1 - p), v(q, q, 1 - p)));

            quads.add(new Rect(v(q, 1 - q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, q, 1 - p), v(q, q, 1 - p)));
            quads.add(new Rect(v(q, 1 - q, 1), v(q, q, 1), v(1 - q, q, 1), v(1 - q, 1 - q, 1)));
        } else {
            quads.add(new Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
        }

        return quads;
    }

    public static void renderRect(BufferBuilder buffer, Rect rect, BlockPos p, float r, float g, float b, float a) {
        buffer.pos(p.getX() + rect.v1.x, p.getY() + rect.v1.y, p.getZ() + rect.v1.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v2.x, p.getY() + rect.v2.y, p.getZ() + rect.v2.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v2.x, p.getY() + rect.v2.y, p.getZ() + rect.v2.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v3.x, p.getY() + rect.v3.y, p.getZ() + rect.v3.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v3.x, p.getY() + rect.v3.y, p.getZ() + rect.v3.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v4.x, p.getY() + rect.v4.y, p.getZ() + rect.v4.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v4.x, p.getY() + rect.v4.y, p.getZ() + rect.v4.z).color(r, g, b, a).endVertex();
        buffer.pos(p.getX() + rect.v1.x, p.getY() + rect.v1.y, p.getZ() + rect.v1.z).color(r, g, b, a).endVertex();
    }


    private static void renderHilightedBlock(RenderWorldLastEvent evt) {
        BlockPos c = XNet.instance.clientInfo.getHilightedBlock();
        if (c == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        long time = System.currentTimeMillis();

        if (time > XNet.instance.clientInfo.getExpireHilight()) {
            XNet.instance.clientInfo.hilightBlock(null, -1);
            return;
        }

        if (((time / 500) & 1) == 0) {
            return;
        }

        EntityPlayerSP p = mc.player;
        double doubleX = p.lastTickPosX + (p.posX - p.lastTickPosX) * evt.getPartialTicks();
        double doubleY = p.lastTickPosY + (p.posY - p.lastTickPosY) * evt.getPartialTicks();
        double doubleZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * evt.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 0, 0);
        GlStateManager.glLineWidth(3);
        GlStateManager.translate(-doubleX, -doubleY, -doubleZ);

        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        float mx = c.getX();
        float my = c.getY();
        float mz = c.getZ();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        mcjty.lib.client.RenderHelper.renderHighLightedBlocksOutline(buffer, mx, my, mz, 1.0f, 0.0f, 0.0f, 1.0f);

        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}

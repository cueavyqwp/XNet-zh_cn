package mcjty.xnet;

import mcjty.xnet.multiblock.XNetWirelessChannels;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ForgeEventHandlers {

    private static final int AMOUNT = 10;

    private int cnt = AMOUNT;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side == Side.SERVER) {
            cnt--;
            if (cnt > 0) {
                return;
            }
            cnt = AMOUNT;

            XNetWirelessChannels data = XNetWirelessChannels.getWirelessChannels(event.world);
            data.tick(event.world, AMOUNT);
        }
    }
}

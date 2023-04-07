package mcjty.xnet.compat;

import mcjty.rftools.api.storage.IStorageScanner;
import mcjty.xnet.api.channels.IControllerContext;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.apiimpl.items.ItemConnectorSettings;
import mcjty.xnet.config.ConfigSetup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RFToolsSupport {

    public static boolean isStorageScanner(TileEntity te) {
        return te instanceof IStorageScanner;
    }

    public static void tickStorageScanner(IControllerContext context, ItemConnectorSettings settings, TileEntity te, ItemChannelSettings channelSettings) {
        IStorageScanner scanner = (IStorageScanner) te;
        Predicate<ItemStack> extractMatcher = settings.getMatcher();

        Integer count = settings.getCount();
        int amount = 0;
        if (count != null) {
            amount = scanner.countItems(extractMatcher, true, count);
            if (amount < count) {
                return;
            }
        }
        int cnt = 0;
        switch (settings.getStackMode()) {
            case SINGLE:
                cnt = 1;
                break;
            case STACK:
                cnt = 64;
                break;
            case COUNT:
                cnt = settings.getExtractAmount();
                break;
        }
        ItemStack stack = scanner.requestItem(extractMatcher, true, cnt, true);
        if (!stack.isEmpty()) {
            // Now that we have a stack we first reduce the amount of the stack if we want to keep a certain
            // number of items
            int toextract = stack.getCount();
            if (count != null) {
                int canextract = amount-count;
                if (canextract <= 0) {
                    return;
                }
                if (canextract < toextract) {
                    toextract = canextract;
                    if (toextract <= 0) {
                        stack.setCount(0);
                    } else {
                        stack.setCount(toextract);
                    }
                }
            }

            List<Pair<SidedConsumer, ItemConnectorSettings>> inserted = new ArrayList<>();
            int remaining = channelSettings.insertStackSimulate(inserted, context, stack);
            if (!inserted.isEmpty()) {
                if (context.checkAndConsumeRF(ConfigSetup.controllerOperationRFT.get())) {
                    channelSettings.insertStackReal(context, inserted, scanner.requestItem(extractMatcher, false, toextract - remaining, true));
                }
            }
        }
    }

    public static int countItems(TileEntity te, Predicate<ItemStack> matcher, int count) {
        IStorageScanner scanner = (IStorageScanner) te;
        return scanner.countItems(matcher, true, count);
    }

    public static int countItems(TileEntity te, ItemStack stack, int count) {
        IStorageScanner scanner = (IStorageScanner) te;
        return scanner.countItems(stack, true, true, count);
    }

    public static ItemStack insertItem(TileEntity te, ItemStack stack, boolean simulate) {
        IStorageScanner scanner = (IStorageScanner) te;
        return scanner.insertItem(stack, simulate);
    }
}

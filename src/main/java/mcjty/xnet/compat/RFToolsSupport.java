package mcjty.xnet.compat;

import mcjty.rftoolsbase.api.storage.IStorageScanner;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.apiimpl.items.ItemConnectorSettings;
import mcjty.xnet.setup.Config;
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
        Predicate<ItemStack> extractMatcher = settings.getMatcher(context);

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
            case 单个物品:
                cnt = 1;
                break;
            case 一组物品:
                cnt = 64;
                break;
            case 指定数量:
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
                if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
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
        return scanner.countItems(stack, true, count);
    }

    public static ItemStack insertItem(TileEntity te, ItemStack stack, boolean simulate) {
        IStorageScanner scanner = (IStorageScanner) te;
        return scanner.insertItem(stack, simulate);
    }
}

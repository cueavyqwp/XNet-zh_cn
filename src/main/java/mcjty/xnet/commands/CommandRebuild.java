package mcjty.xnet.commands;

import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandRebuild implements ICommand {

    @Override
    public String getName() {
        return "xnetrebuild";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return getName();
    }


    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }


    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        XNetBlobData data = XNetBlobData.getBlobData(server.getEntityWorld());
        data.getWorldBlob(sender.getEntityWorld()).recalculateNetwork();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return new ArrayList<>();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public boolean isUsernameIndex(String[] sender, int p_82358_2_) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo(o.getName());
    }
}

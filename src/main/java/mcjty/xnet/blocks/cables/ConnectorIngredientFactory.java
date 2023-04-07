package mcjty.xnet.blocks.cables;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class ConnectorIngredientFactory implements IIngredientFactory {
    @Nonnull
    @Override
    public Ingredient parse(JsonContext context, JsonObject json) {
        return Ingredient.fromStacks(
                new ItemStack(NetCableSetup.connectorBlock, 1, 0),
                new ItemStack(NetCableSetup.connectorBlock, 1, 1),
                new ItemStack(NetCableSetup.connectorBlock, 1, 2),
                new ItemStack(NetCableSetup.connectorBlock, 1, 3)
        );
    }
}

package mcjty.xnet.blocks.cables;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class AdvancedConnectorIngredientFactory implements IIngredientFactory {
    @Nonnull
    @Override
    public Ingredient parse(JsonContext context, JsonObject json) {
        return Ingredient.fromStacks(
                new ItemStack(NetCableSetup.advancedConnectorBlock, 1, 0),
                new ItemStack(NetCableSetup.advancedConnectorBlock, 1, 1),
                new ItemStack(NetCableSetup.advancedConnectorBlock, 1, 2),
                new ItemStack(NetCableSetup.advancedConnectorBlock, 1, 3)
        );
    }
}

package com.lucidaps.odailyquests.commands.interfaces.playerinterface.items.getters;

import com.lucidaps.odailyquests.tools.Pair;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface IItemGetter {
    Pair<String, ItemStack> getCustomModelDataItem(Material material, int customModelData);
    Pair<String, ItemStack> getItemModelItem(String itemModel);
}

package com.lucidaps.odailyquests.tools.externals;

import com.lucidaps.odailyquests.tools.Pair;
import org.bukkit.inventory.ItemStack;

public interface IExternalItemGetter {

    Pair<String, ItemStack> getNexoItem(String namespace);
    Pair<String, ItemStack> getCustomHead(String texture);
}

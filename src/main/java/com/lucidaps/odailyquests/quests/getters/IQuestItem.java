package com.lucidaps.odailyquests.quests.getters;

import org.bukkit.inventory.ItemStack;

public interface IQuestItem {

    ItemStack getItem(String material, String fileName, String questIndex, String parameter);
    ItemStack getNexoItem(String namespace, String fileName, String questIndex, String parameter);
    ItemStack getCustomHead(String texture, String fileName, String questIndex, String parameter);
    ItemStack getCustomModelDataItem(String customModelData, String fileName, String questIndex, String parameter);
    ItemStack getItemModelItem(String itemModel, String fileName, String questIndex, String parameter);
}

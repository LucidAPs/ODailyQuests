package com.lucidaps.odailyquests.commands.interfaces.holder;

import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record CategoryHolder(int page, String category, QuestPeriod period) implements InventoryHolder {

    @NotNull
    @Override
    public Inventory getInventory() {
        return null;
    }
}

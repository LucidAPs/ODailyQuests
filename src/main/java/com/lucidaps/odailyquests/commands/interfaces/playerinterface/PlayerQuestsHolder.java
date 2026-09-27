package com.lucidaps.odailyquests.commands.interfaces.playerinterface;

import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuestsHolder implements InventoryHolder {

    private final QuestPeriod period;

    public PlayerQuestsHolder(QuestPeriod period) {
        this.period = period;
    }

    public QuestPeriod getPeriod() {
        return period;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return null;
    }
}

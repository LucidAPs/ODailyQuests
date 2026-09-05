package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class PickupQuest extends ItemQuest {

    public PickupQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "PICKUP";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof EntityPickupItemEvent event) {
            return super.isRequiredItem(event.getItem().getItemStack(), progression);
        }

        return false;
    }
}

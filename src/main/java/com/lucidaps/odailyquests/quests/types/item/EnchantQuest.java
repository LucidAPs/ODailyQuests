package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;

public class EnchantQuest extends ItemQuest {

    public EnchantQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "ENCHANT";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof EnchantItemEvent event) {
            return super.isRequiredItem(event.getItem(), progression);
        }

        return false;
    }
}

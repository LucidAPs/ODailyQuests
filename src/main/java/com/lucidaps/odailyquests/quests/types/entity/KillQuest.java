package com.lucidaps.odailyquests.quests.types.entity;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.EntityQuest;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillQuest extends EntityQuest {

    public KillQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "KILL";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof EntityDeathEvent event) {
            return super.isRequiredEntity(event.getEntity().getType(), progression);
        }

        return false;
    }
}

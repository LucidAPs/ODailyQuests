package com.lucidaps.odailyquests.quests.types.entity;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.EntityQuest;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTameEvent;

public class TameQuest extends EntityQuest {

    public TameQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "TAME";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        if (provided instanceof EntityTameEvent event) {
            return super.isRequiredEntity(event.getEntity().getType(), progression);
        }

        return false;
    }
}

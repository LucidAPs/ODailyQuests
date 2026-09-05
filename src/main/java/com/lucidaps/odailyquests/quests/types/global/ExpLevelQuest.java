package com.lucidaps.odailyquests.quests.types.global;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLevelChangeEvent;

public class ExpLevelQuest extends AbstractQuest {

    public ExpLevelQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "EXP_LEVELS";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        return provided instanceof PlayerLevelChangeEvent;
    }

    @Override
    public boolean loadParameters(ConfigurationSection section, String file, String index) {
        return true;
    }
}

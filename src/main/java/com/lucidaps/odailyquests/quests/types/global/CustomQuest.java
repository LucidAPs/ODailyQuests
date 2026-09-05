package com.lucidaps.odailyquests.quests.types.global;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CustomQuest extends AbstractQuest {

    public CustomQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return this.getQuestType();
    }

    @Override
    public boolean canProgress(@Nullable Event provided, Progression progression) {
        return true;
    }

    @Override
    public boolean loadParameters(ConfigurationSection section, String file, String index) {
        return true;
    }

}

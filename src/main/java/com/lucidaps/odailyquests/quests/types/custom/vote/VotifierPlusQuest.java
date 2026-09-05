package com.lucidaps.odailyquests.quests.types.custom.vote;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.PluginUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;

public class VotifierPlusQuest extends AbstractQuest {

    private static final String VOTIFIER_EVENT_CLASS = "com.vexsoftware.votifier.model.VotifierEvent";

    public VotifierPlusQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "VOTIFIER_PLUS";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        return isVotifierEvent(provided.getClass());
    }

    @Override
    public boolean loadParameters(ConfigurationSection section, String file, String index) {
        if (!PluginUtils.isPluginEnabled("VotifierPlus")) {
            PluginLogger.configurationError(file, index, null, "You must have VotifierPlus installed to use this quest.");
            return false;
        }

        return true;
    }

    private boolean isVotifierEvent(Class<?> eventClass) {
        Class<?> currentClass = eventClass;
        while (currentClass != null) {
            if (VOTIFIER_EVENT_CLASS.equals(currentClass.getName())) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }
}

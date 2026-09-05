package com.lucidaps.odailyquests.quests.types.item;

import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.BasicQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;

import java.util.Locale;

public class VillagerQuest extends ItemQuest {

    Villager.Profession profession;
    int level;

    public VillagerQuest(BasicQuest base) {
        super(base);
    }

    @Override
    public String getType() {
        return "VILLAGER_TRADE";
    }

    @Override
    public boolean canProgress(Event provided, Progression progression) {
        return false;
    }

    @Override
    public boolean loadParameters(ConfigurationSection section, String file, String index) {
        if (!super.loadParameters(section, file, index)) return false;

        /* check if the item have to be obtained by a villager */
        if (section.contains(".villager_profession")) {
            final String configuredProfession = section.getString(".villager_profession");
            if (configuredProfession == null) {
                PluginLogger.configurationError(file, index, "villager_profession", "Villager profession is not defined.");
                return false;
            }

            final String professionKey = configuredProfession.trim().toLowerCase(Locale.ROOT);
            final NamespacedKey namespacedKey = professionKey.contains(":")
                    ? NamespacedKey.fromString(professionKey)
                    : NamespacedKey.minecraft(professionKey);
            if (namespacedKey == null) {
                PluginLogger.configurationError(file, index, "villager_profession", "Invalid villager profession.");
                return false;
            }

            profession = Registry.VILLAGER_PROFESSION.get(namespacedKey);
            if (profession == null) {
                PluginLogger.configurationError(file, index, "villager_profession", "Invalid villager profession.");
                return false;
            }
        }
        if (section.contains(".villager_level")) {
            level = section.getInt(".villager_level");
        }

        return true;
    }

    /**
     * Get required villager profession
     *
     * @return villager profession
     */
    public Villager.Profession getVillagerProfession() {
        return this.profession;
    }

    /**
     * Get required villager level
     *
     * @return villager level
     */
    public int getVillagerLevel() {
        return this.level;
    }
}

package com.lucidaps.odailyquests.quests.categories;

import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import java.util.ArrayList;

public class Category extends ArrayList<AbstractQuest> {

    private final String name;
    private final QuestPeriod period;

    public Category(String name, QuestPeriod period) {
        this.name = name;
        this.period = period;
    }

    /**
     * Get the name of the category.
     * @return name of the category.
     */
    public String getName() {
        return this.name;
    }

    public QuestPeriod getPeriod() {
        return period;
    }
}

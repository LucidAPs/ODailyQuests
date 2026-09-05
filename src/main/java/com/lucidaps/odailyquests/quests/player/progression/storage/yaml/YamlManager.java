package com.lucidaps.odailyquests.quests.player.progression.storage.yaml;

import com.lucidaps.odailyquests.files.implementations.ProgressionFile;

public class YamlManager {

    private final ProgressionFile progressionFile;
    private final LoadProgressionYAML loadProgressionYAML;
    private final SaveProgressionYAML saveProgressionYAML;

    public YamlManager(ProgressionFile progressionFile) {
        this.progressionFile = progressionFile;
        this.loadProgressionYAML = new LoadProgressionYAML(progressionFile);
        this.saveProgressionYAML = new SaveProgressionYAML(progressionFile);
    }

    /**
     * Get LoadProgressionYAML instance.
     * @return LoadProgressionYAML instance.
     */
    public LoadProgressionYAML getLoadProgressionYAML() {
        return loadProgressionYAML;
    }

    /**
     * Get SaveProgressionYAML instance.
     * @return SaveProgressionYAML instance.
     */
    public SaveProgressionYAML getSaveProgressionYAML() {
        return saveProgressionYAML;
    }

    public ProgressionFile getProgressionFile() {
        return progressionFile;
    }
}

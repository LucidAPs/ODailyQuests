package com.lucidaps.odailyquests.quests.player;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.configuration.essentials.QuestsPerCategory;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.categories.Category;
import com.lucidaps.odailyquests.quests.conditions.placeholder.PlaceholderRuleSetEvaluator;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.types.shared.EntityQuest;
import com.lucidaps.odailyquests.quests.types.shared.ItemQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for players' quests lifecycle: loading on join, saving on quit,
 * and utilities to build / pick quests and progressions.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Registering join/quit listeners to load/save player quests via the database manager.</li>
 *   <li>Keeping an in-memory map ({@code activeQuests}) of online players to their {@link PlayerQuests}.</li>
 *   <li>Providing static helpers to select random quests and create fresh {@link Progression} objects.</li>
 * </ul>
 * <p>
 * <strong>Thread-safety:</strong> Bukkit events are called on the main thread.
 * {@code activeQuests} uses a concurrent map because load/save work may cross scheduler boundaries.
 */
public class QuestsManager implements Listener {

    /** RNG used for dynamic amounts and random selections. */
    private static final Random random = new Random();

    /**
     * Main plugin entry point used to reach services such as the database manager.
     * <p>
     * Kept {@code private} to preserve encapsulation (this is not a data carrier/record).
     */
    private final ODailyQuests plugin;

    /**
     * Creates a new {@link QuestsManager}.
     *
     * @param oDailyQuests main plugin instance; must not be {@code null}.
     */
    public QuestsManager(ODailyQuests oDailyQuests) {
        this.plugin = oDailyQuests;
    }

    /**
     * In-memory registry of online players' quests, keyed by player name.
     * <p>
     * <strong>Lifecycle:</strong>
     * <ul>
     *   <li>Inserted (indirectly) when the DB manager loads quests at join.</li>
     *   <li>Saved and removed on quit.</li>
     * </ul>
     * <strong>Note:</strong> Keys are player names; if you plan to support name changes,
     * consider switching to UUID as the key.
     */
    private static final Map<QuestPeriod, Map<String, PlayerQuests>> activeQuests = new EnumMap<>(QuestPeriod.class);
    private static final Map<String, Integer> overallLifetimeTotals = new ConcurrentHashMap<>();
    private static final Set<String> loadedPlayers = ConcurrentHashMap.newKeySet();

    static {
        for (QuestPeriod period : QuestPeriod.values()) {
            activeQuests.put(period, new ConcurrentHashMap<>());
        }
    }

    /**
     * Handles player join:
     * <ol>
     *   <li>Logs debug info.</li>
     *   <li>If the player is not registered in {@link #activeQuests}, trigger async/sync load via DB manager.</li>
     *   <li>If already present, warn (unexpected state) to help diagnose stuck state.</li>
     * </ol>
     *
     * @param event Bukkit player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Debugger.write("[EVENT START]");
        Debugger.write("PlayerJoinEvent triggered.");

        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final UUID uuid = player.getUniqueId();

        Debugger.write("Player " + playerName + " joined the server.");
        Debugger.write("Player UUID is " + uuid);

        if (!loadedPlayers.contains(playerName)) {
            Debugger.write("Player " + playerName + " is not in the array.");
            // Delegates to DB layer: expected to eventually populate activeQuests.
            plugin.getDatabaseManager().loadQuestsForPlayer(playerName);
        } else {
            Debugger.write("Player " + playerName + " is already in the array.");
            PluginLogger.warn(playerName + " detected into the array. This is not supposed to happen!");
            PluginLogger.warn("If the player can't make his quests progress, please contact the plugin developer.");
        }
    }

    /**
     * Handles player quit:
     * <ol>
     *   <li>Logs debug info.</li>
     *   <li>Fetches the player's {@link PlayerQuests} from {@link #activeQuests}.</li>
     *   <li>If found, persists progression then removes the entry.</li>
     *   <li>If not found, logs a warning (unexpected state).</li>
     * </ol>
     *
     * @param event Bukkit player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Debugger.write("[EVENT START]");
        Debugger.write("PlayerQuitEvent triggered.");

        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final String playerUUID = player.getUniqueId().toString();

        Debugger.write("Player " + playerName + " left the server.");

        if (!loadedPlayers.contains(playerName)) {
            Debugger.write("Player " + playerName + " not found in the array.");
            PluginLogger.warn("Player quests not found for player " + playerName);
            return;
        }

        plugin.getDatabaseManager().saveProgressionForPlayer(playerName, playerUUID);
        removePlayer(playerName);

        Debugger.write("Player " + playerName + " removed from the array.");
    }

    /**
     * Selects random quests for a player across all categories.
     * <p>
     * For each registered category, this method:
     * <ol>
     *   <li>Determines the required amount from {@link QuestsPerCategory}.</li>
     *   <li>Picks non-duplicate quests the player has permission to do.</li>
     *   <li>Creates a fresh {@link Progression} per quest.</li>
     * </ol>
     *
     * @param player target player (used for permission checks and logging)
     * @return an ordered map ({@link LinkedHashMap}) of quests to their initial progression
     */
    public static Map<AbstractQuest, Progression> selectRandomQuests(Player player) {
        return selectRandomQuests(player, QuestPeriod.DAILY);
    }

    public static Map<AbstractQuest, Progression> selectRandomQuests(Player player, QuestPeriod period) {
        final Map<AbstractQuest, Progression> quests = new LinkedHashMap<>();

        final Map<String, Category> categoryMap = CategoriesLoader.getAllCategories(period);
        final Map<String, Integer> resolvedAmounts = QuestPeriods.resolveQuestAmounts(period, player);

        for (Map.Entry<String, Category> entry : categoryMap.entrySet()) {
            final String categoryName = entry.getKey();
            final Category category = entry.getValue();

            int requiredAmount = resolvedAmounts.getOrDefault(categoryName, 0);
            if (requiredAmount <= 0) {
                continue;
            }

            for (int i = 0; i < requiredAmount; i++) {
                final AbstractQuest quest = getRandomQuestForPlayer(quests.keySet(), category, player);
                if (quest == null) {
                    Debugger.write("Not enough quests available to assign to " + player.getName() + " in category " + categoryName + ".");
                    break;
                }

                final Progression progression = createFreshProgression(quest);
                quests.put(quest, progression);
            }
        }

        return quests;
    }

    /**
     * Resolves a dynamic "required amount" from a configuration string.
     * Supports fixed integer values (e.g., {@code "5"}) and ranges (e.g., {@code "2-6"}).
     *
     * @param requiredAmountRaw raw value from configuration
     * @return a positive integer (minimum 1)
     * @throws NumberFormatException if the input cannot be parsed as integers
     */
    public static int getDynamicRequiredAmount(String requiredAmountRaw) {
        if (requiredAmountRaw.contains("-")) {
            String[] parts = requiredAmountRaw.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            if (min < 1) min = 1;

            return random.nextInt(min, max + 1);
        }

        int amount = Integer.parseInt(requiredAmountRaw);
        return Math.max(amount, 1);
    }

    /**
     * Computes a random index inside a quest's required targets, depending on its type.
     * <ul>
     *   <li>{@link EntityQuest}: random entity index</li>
     *   <li>{@link ItemQuest}: random item index</li>
     *   <li>Other quests: returns 0</li>
     * </ul>
     *
     * @param quest the quest to inspect
     * @return a valid index for the quest's "required" list, or 0 if not applicable
     */
    private static int getRandomIndexFrom(AbstractQuest quest) {
        if (quest instanceof EntityQuest eq) {
            return random.nextInt(eq.getRequiredEntities().size());
        }

        if (quest instanceof ItemQuest iq) {
            return random.nextInt(iq.getRequiredItems().size());
        }

        return 0;
    }

    /**
     * Creates a fresh {@link Progression} for the provided quest.
     * <p>
     * The progression:
     * <ul>
     *   <li>has a required amount resolved by {@link #getDynamicRequiredAmount(String)}</li>
     *   <li>starts at 0 progress, not achieved</li>
     *   <li>optionally sets a random required index when {@link AbstractQuest#isRandomRequired()} is true</li>
     * </ul>
     *
     * @param quest the quest to build a progression for; must not be {@code null}
     * @return a new {@link Progression} initialised for the quest
     */
    public static Progression createFreshProgression(AbstractQuest quest) {
        final int requiredAmount = getDynamicRequiredAmount(quest.getRequiredAmountRaw());
        final double rewardAmount = quest.getReward().resolveRewardAmount();
        final Progression progression = new Progression(requiredAmount, rewardAmount, 0, false);

        if (quest.isRandomRequired()) {
            progression.setSelectedRequiredIndex(getRandomIndexFrom(quest));
        }

        return progression;
    }

    /**
     * Picks a random quest from a provided list that:
     * <ul>
     *   <li>is not already present in {@code currentQuests}</li>
     *   <li>meets all permission requirements for the given player</li>
     * </ul>
     *
     * @param currentQuests   set of quests already assigned to the player (for duplicate filtering)
     * @param availableQuests candidate quests to pick from
     * @param player          player used for permission checks
     * @return a random eligible quest, or {@code null} if none are eligible
     */
    public static AbstractQuest getRandomQuestForPlayer(Set<AbstractQuest> currentQuests, List<AbstractQuest> availableQuests, Player player) {
        final List<AbstractQuest> filteredQuests = new ArrayList<>();

        for (AbstractQuest quest : availableQuests) {
            if (hasAllPermissions(player, quest.getRequiredPermissions())
                    && !currentQuests.contains(quest)
                    && PlaceholderRuleSetEvaluator.evaluate(player, quest.getPlaceholderConditions(), false)) {
                filteredQuests.add(quest);
            }
        }

        if (filteredQuests.isEmpty()) {
            return null;
        }

        int randomIndex = random.nextInt(filteredQuests.size());
        return filteredQuests.get(randomIndex);
    }

    /**
     * Checks whether a player has all permissions in the provided list.
     *
     * @param player      the player to check (must not be {@code null})
     * @param permissions list of permissions; {@code null} or empty means "no restriction"
     * @return {@code true} if all permissions are granted or none are required; {@code false} otherwise
     */
    private static boolean hasAllPermissions(Player player, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        for (String permission : permissions) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Exposes the in-memory map of active quests.
     * <p>
     * Mutations on the returned map affect the internal state directly.
     * Prefer adding dedicated methods if you need stricter control.
     *
     * @return the live map of player name -&gt; {@link PlayerQuests}
     */
    public static Map<String, PlayerQuests> getActiveQuests() {
        return getActiveQuests(QuestPeriod.DAILY);
    }

    public static Map<String, PlayerQuests> getActiveQuests(QuestPeriod period) {
        return activeQuests.get(period);
    }

    public static PlayerQuests getPlayerQuests(String playerName, QuestPeriod period) {
        return getActiveQuests(period).get(playerName);
    }

    public static void registerPlayerPeriod(String playerName, QuestPeriod period, PlayerQuests playerQuests) {
        getActiveQuests(period).put(playerName, playerQuests);
    }

    public static void markPlayerLoaded(String playerName, int overallTotal) {
        overallLifetimeTotals.put(playerName, overallTotal);
        loadedPlayers.add(playerName);
    }

    public static boolean isPlayerLoaded(String playerName) {
        return loadedPlayers.contains(playerName);
    }

    public static Set<String> getLoadedPlayers() {
        return Set.copyOf(loadedPlayers);
    }

    public static int getOverallLifetimeTotal(String playerName) {
        return overallLifetimeTotals.getOrDefault(playerName, 0);
    }

    public static void setOverallLifetimeTotal(String playerName, int total) {
        overallLifetimeTotals.put(playerName, Math.max(total, 0));
    }

    public static int incrementOverallLifetimeTotal(String playerName) {
        return overallLifetimeTotals.merge(playerName, 1, Integer::sum);
    }

    public static void removePlayer(String playerName) {
        for (Map<String, PlayerQuests> periodQuests : activeQuests.values()) {
            periodQuests.remove(playerName);
        }
        overallLifetimeTotals.remove(playerName);
        loadedPlayers.remove(playerName);
    }
}

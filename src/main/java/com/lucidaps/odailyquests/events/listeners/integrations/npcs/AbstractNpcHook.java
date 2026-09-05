package com.lucidaps.odailyquests.events.listeners.integrations.npcs;

import com.lucidaps.odailyquests.commands.interfaces.InterfacesManager;
import com.lucidaps.odailyquests.configuration.integrations.NPCNames;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

public abstract class AbstractNpcHook implements Listener {

    private static final String PERMISSION_PREFIX = "odailyquests.";
    protected final InterfacesManager interfacesManager;

    protected AbstractNpcHook(InterfacesManager interfacesManager) {
        this.interfacesManager = Objects.requireNonNull(interfacesManager, "interfacesManager");
    }

    /**
     * Handles an interaction with a quest NPC.
     * <p>
     * Depending on the NPC name, this method either:
     * <ul>
     *   <li>opens the player quests interface (player NPC)</li>
     *   <li>opens the first page of a category interface (category NPC)</li>
     * </ul>
     * If the player lacks the required permission, a no-permission message is sent.
     *
     * @param npcName the clicked NPC name
     * @param player  the interacting player
     */
    protected final void handle(String npcName, Player player) {
        if (npcName == null || player == null) return;

        if (isPlayerNpc(npcName)) {
            openPlayerInterface(player);
            return;
        }

        final String category = resolveCategory(npcName);
        if (category == null) return;

        openCategoryInterface(player, category);
    }

    /**
     * Checks whether the given NPC name matches the configured player NPC name.
     *
     * @param npcName the NPC name
     * @return true if it is the player NPC, otherwise false
     */
    private boolean isPlayerNpc(String npcName) {
        return npcName.equals(NPCNames.getPlayerNPCName());
    }

    /**
     * Resolves the quest category linked to the given NPC name.
     *
     * @param npcName the NPC name
     * @return the category name, or null if the NPC is not a category NPC
     */
    private String resolveCategory(String npcName) {
        if (!NPCNames.isCategoryForNPCName(npcName)) {
            return null;
        }
        return NPCNames.getCategoryByNPCName(npcName);
    }

    /**
     * Opens the player quests interface if the player has permission.
     *
     * @param player the player
     */
    private void openPlayerInterface(Player player) {
        if (!hasPermission(player, "player")) {
            sendNoPermission(player);
            return;
        }

        final Inventory inv = interfacesManager.getPlayerQuestsInterface()
                .getPlayerQuestsInterface(player);

        openInventoryIfNotNull(player, inv);
    }

    /**
     * Opens the first page of the given category interface if the player has permission.
     *
     * @param player   the player
     * @param category the category name
     */
    private void openCategoryInterface(Player player, String category) {
        if (!hasPermission(player, category)) {
            sendNoPermission(player);
            return;
        }

        final Inventory inv = interfacesManager.getQuestsInterfaces()
                .getInterfaceFirstPage(category, player);

        if (inv == null) {
            sendMessage(player, QuestsMessages.CONFIGURATION_ERROR);
            return;
        }

        player.openInventory(inv);
    }

    /**
     * Checks a permission with the common ODailyQuests prefix.
     *
     * @param player the player
     * @param node   the permission node suffix
     * @return true if the player has the permission
     */
    private boolean hasPermission(Player player, String node) {
        return player.hasPermission(PERMISSION_PREFIX + node);
    }

    /**
     * Opens an inventory if it is not null.
     *
     * @param player the player
     * @param inv    the inventory to open
     */
    private void openInventoryIfNotNull(Player player, Inventory inv) {
        if (inv != null) {
            player.openInventory(inv);
        }
    }

    /**
     * Sends a localized message to the player if available.
     *
     * @param player  the player
     * @param message the message enum
     */
    protected void sendMessage(Player player, QuestsMessages message) {
        final String msg = message.toString();
        if (msg != null) {
            player.sendMessage(msg);
        }
    }

    /**
     * Sends a message to the player when they do not have permission to open the NPC interface.
     *
     * @param player the player
     */
    protected void sendNoPermission(Player player) {
        sendMessage(player, QuestsMessages.NO_PERMISSION_CATEGORY);
    }
}

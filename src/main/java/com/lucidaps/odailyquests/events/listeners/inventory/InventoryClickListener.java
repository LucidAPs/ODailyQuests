package com.lucidaps.odailyquests.events.listeners.inventory;

import com.lucidaps.odailyquests.commands.interfaces.playerinterface.PlayerQuestsHolder;
import com.lucidaps.odailyquests.commands.interfaces.playerinterface.PlayerQuestsInterface;
import com.lucidaps.odailyquests.configuration.essentials.CustomFurnaceResults;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.events.customs.CustomFurnaceExtractEvent;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.clickable.ClickableChecker;
import com.lucidaps.odailyquests.quests.player.progression.clickable.QuestContext;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.List;

public class InventoryClickListener extends ClickableChecker implements Listener {

    private final PlayerQuestsInterface playerQuestsInterface;

    public InventoryClickListener(PlayerQuestsInterface playerQuestsInterface) {
        this.playerQuestsInterface = playerQuestsInterface;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickEvent(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        final InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING) return;

        final Player player = (Player) event.getWhoClicked();
        if (!QuestsManager.isPlayerLoaded(player.getName())) {
            return;
        }

        boolean isPlayerInterface = false;

        final Inventory top = event.getView().getTopInventory();
        PlayerQuestsHolder playerQuestsHolder = null;
        if (top.getHolder() instanceof PlayerQuestsHolder holder) {
            playerQuestsHolder = holder;
            isPlayerInterface = true;
            event.setCancelled(true);
        }

        final ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (handleCustomFurnaceResult(event, action, clickedItem, player)) return;

        final QuestContext.Builder contextBuilder = new QuestContext.Builder(player).clickedItem(clickedItem);
        if (playerQuestsHolder != null) contextBuilder.period(playerQuestsHolder.getPeriod());
        if (handleVillagerTrading(event, contextBuilder)) {
            return;
        }

        // do action related to the clicked item
        if (isPlayerInterface) {
            if (handlePlayerInterfaceClick(event, clickedItem, player)) return;
            processQuestCompletion(contextBuilder.build());
        }
    }

    private boolean handlePlayerInterfaceClick(InventoryClickEvent event, ItemStack clickedItem, Player player) {
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) return true;
        if (playerQuestsInterface.isFillItem(clickedItem)) return true;

        final int slot = event.getRawSlot();
        if (handlePlayerCommandItem(player, slot)) return true;
        if (handleConsoleCommandItem(player, slot)) return true;
        return handleCloseItem(clickedItem, player);
    }

    /* ======================  VILLAGER TRADE (refactor)  ====================== */

    private boolean handleVillagerTrading(InventoryClickEvent event, QuestContext.Builder contextBuilder) {
        if (!isVillagerResultSlot(event)) return false;

        Debugger.write("Detected villager trade action");
        final MerchantInventory merchantInventory = (MerchantInventory) event.getClickedInventory();
        if (merchantInventory == null) {
            Debugger.write("Merchant Inventory is null");
            return true;
        }

        if (event.getClickedInventory().getHolder() instanceof Villager villager) {
            processVillagerTrade(event, merchantInventory, villager, contextBuilder);
        }
        return true;
    }

    private boolean isVillagerResultSlot(InventoryClickEvent event) {
        return event.getInventory().getType() == InventoryType.MERCHANT
                && event.getSlotType() == InventoryType.SlotType.RESULT;
    }

    private void processVillagerTrade(InventoryClickEvent event, MerchantInventory merchantInventory, Villager villager, QuestContext.Builder contextBuilder) {
        final Merchant merchant = merchantInventory.getMerchant();
        final MerchantRecipe selectedRecipe = merchantInventory.getSelectedRecipe();

        if (selectedRecipe == null) {
            Debugger.write("Merchant or selected recipe is null");
            return;
        }

        final int perTradeResult = Math.max(1, selectedRecipe.getResult().getAmount());
        final int idx = findRecipeIndex(merchant, selectedRecipe);

        if (idx < 0) {
            Debugger.write("Cannot find selected recipe index in merchant list; fallback per click.");
            contextBuilder.villagerTrade(villager, selectedRecipe, perTradeResult);
            processQuestCompletion(contextBuilder.build());
            return;
        }

        final int beforeUses = merchant.getRecipe(idx).getUses();
        Debugger.write("[TradeDelta] Before uses=" + beforeUses
                + ", perTradeResult=" + perTradeResult
                + ", click=" + event.getClick()
                + ", action=" + event.getAction());

        scheduleDeltaProgress(merchant, idx, beforeUses, perTradeResult, villager, selectedRecipe, contextBuilder);
    }

    private void scheduleDeltaProgress(Merchant merchant, int idx, int beforeUses, int perTradeResult, Villager villager, MerchantRecipe selectedRecipe, QuestContext.Builder contextBuilder) {
        TaskScheduler.runSyncLater(
                () -> {
                    if (!villager.isValid()) {
                        Debugger.write("[TradeDelta] Skipped: villager is no longer valid. Fallback per-click.");
                        contextBuilder.villagerTrade(villager, selectedRecipe, perTradeResult);
                        processQuestCompletion(contextBuilder.build());
                        return;
                    }

                    try {
                        final MerchantRecipe afterRec = merchant.getRecipe(idx);
                        final int afterUses = afterRec.getUses();
                        final int deltaTrades = Math.max(0, afterUses - beforeUses);
                        Debugger.write("[TradeDelta] After uses=" + afterUses + " -> deltaTrades=" + deltaTrades);

                        if (deltaTrades == 0) {
                            Debugger.write("[TradeDelta] No trades executed (delta=0). No quest progress.");
                            return;
                        }

                        final int amount = deltaTrades * perTradeResult;
                        Debugger.write("[TradeDelta] Final quest amount=" + amount
                                + " (deltaTrades=" + deltaTrades + " * perTradeResult=" + perTradeResult + ")");

                        contextBuilder.villagerTrade(villager, selectedRecipe, amount);
                        processQuestCompletion(contextBuilder.build());
                    } catch (Exception e) {
                        Debugger.write("[TradeDelta] ERROR while reading after-uses: " + e.getMessage());
                        contextBuilder.villagerTrade(villager, selectedRecipe, perTradeResult);
                        processQuestCompletion(contextBuilder.build());
                    }
                },
                1L
        );
    }

    /* ======================  FURNACE  ====================== */

    private boolean handleCustomFurnaceResult(InventoryClickEvent event, InventoryAction action, ItemStack clickedItem, Player player) {
        if (!CustomFurnaceResults.isEnabled()) return false;

        final InventoryType inventoryType = event.getInventory().getType();
        if (inventoryType != InventoryType.FURNACE
                && inventoryType != InventoryType.BLAST_FURNACE
                && inventoryType != InventoryType.SMOKER) {
            return false;
        }

        if (event.getSlotType() != InventoryType.SlotType.RESULT) return true;

        int amount;
        switch (action) {
            case PICKUP_HALF -> amount = (int) Math.ceil(clickedItem.getAmount() / 2.0);
            case PICKUP_ONE, DROP_ONE_SLOT -> amount = 1;
            case MOVE_TO_OTHER_INVENTORY -> {
                int max = clickedItem.getAmount();
                amount = Math.min(max, fits(clickedItem, player.getInventory().getStorageContents()));
            }
            default -> amount = clickedItem.getAmount();
        }

        if (amount == 0) return true;

        final CustomFurnaceExtractEvent customFurnaceExtractEvent = new CustomFurnaceExtractEvent(player, clickedItem, amount);
        Bukkit.getServer().getPluginManager().callEvent(customFurnaceExtractEvent);

        return true;
    }

    /* ======================  RECIPE MATCHING  ====================== */

    private int findRecipeIndex(Merchant merchant, MerchantRecipe target) {
        final List<MerchantRecipe> list = merchant.getRecipes();
        for (int i = 0; i < list.size(); i++) {
            final MerchantRecipe r = list.get(i);
            if (sameRecipe(r, target)) return i;
        }
        return -1;
    }

    private boolean sameRecipe(MerchantRecipe a, MerchantRecipe b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (areDifferentItems(a.getResult(), b.getResult())) return false;

        final List<ItemStack> ia = a.getIngredients();
        final List<ItemStack> ib = b.getIngredients();
        if (ia.size() != ib.size()) return false;

        for (int i = 0; i < ia.size(); i++) {
            if (areDifferentItems(ia.get(i), ib.get(i))) return false;
        }
        return true;
    }

    private boolean areDifferentItems(ItemStack x, ItemStack y) {
        return !areSameItem(x, y);
    }

    private boolean areSameItem(ItemStack x, ItemStack y) {
        if (x == y) return true;
        if (x == null || y == null) return false;
        try {
            return x.isSimilar(y) && x.getAmount() == y.getAmount();
        } catch (Exception e) {
            Debugger.write("[areSameItem] ERROR while checking item equality: " + e);
            return false;
        }
    }

    /* ======================  UI items  ====================== */

    private boolean handleCloseItem(ItemStack clickedItem, Player player) {
        if (playerQuestsInterface.isCloseItem(clickedItem)) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    private boolean handlePlayerCommandItem(Player player, int slot) {
        if (playerQuestsInterface.isPlayerCommandItem(slot)) {
            for (String cmd : playerQuestsInterface.getPlayerCommands(slot)) {
                Bukkit.getServer().dispatchCommand(player, cmd);
            }
            if (playerQuestsInterface.shouldCloseOnClick(slot)) {
                player.closeInventory();
            }
            return true;
        }
        return false;
    }

    private boolean handleConsoleCommandItem(Player player, int slot) {
        if (playerQuestsInterface.isConsoleCommandItem(slot)) {
            for (String cmd : playerQuestsInterface.getConsoleCommands(slot)) {
                TaskScheduler.runSync(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName())));
            }
            if (playerQuestsInterface.shouldCloseOnClick(slot)) {
                player.closeInventory();
            }
            return true;
        }
        return false;
    }
}

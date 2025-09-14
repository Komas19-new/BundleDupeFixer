package party.komas19.bundleDupeFixer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BundleDupeFixer extends JavaPlugin implements Listener {

    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, Integer> useCount = new HashMap<>();

    private int maxActions;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxActions = getConfig().getInt("max-actions-per-second", 5);
        messageManager = new MessageManager(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BundleDupeFixer enabled with limit: " + maxActions + " actions/sec");
    }

    private boolean isBundle(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_BUNDLE") || type == Material.BUNDLE;
    }

    private boolean rateLimit(Player player, String action) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        long last = lastUse.getOrDefault(uuid, 0L);
        int count = useCount.getOrDefault(uuid, 0);

        if (now - last > 1000) {
            count = 0;
            last = now;
        }

        count++;
        lastUse.put(uuid, now);
        useCount.put(uuid, count);

        if (count > maxActions) {
            Map<String, String> placeholders = Map.of(
                    "player", player.getName(),
                    "action", action
            );

            String msg = messageManager.getMessage("alerts.rate-limit", placeholders);
            getLogger().warning(player.getName() + " triggered bundle rate-limit (" + action + ")");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("bundledupefixer.alert")) {
                    online.sendMessage(msg);
                }
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (isBundle(e.getItem()) && rateLimit(e.getPlayer(), "interact")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(messageManager.getMessage("player-messages.interact", Map.of()));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isBundle(e.getItemDrop().getItemStack()) && rateLimit(e.getPlayer(), "drop")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(messageManager.getMessage("player-messages.drop", Map.of()));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        if (isBundle(e.getCurrentItem()) || isBundle(e.getCursor())) {
            if (rateLimit(player, "inventory-click")) {
                e.setCancelled(true);
                player.sendMessage(messageManager.getMessage("player-messages.inventory-click", Map.of()));
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        boolean hasBundle = e.getOldCursor() != null && isBundle(e.getOldCursor());
        if (!hasBundle) {
            for (ItemStack item : e.getNewItems().values()) {
                if (isBundle(item)) {
                    hasBundle = true;
                    break;
                }
            }
        }
        if (hasBundle && rateLimit(player, "inventory-drag")) {
            e.setCancelled(true);
            player.sendMessage(messageManager.getMessage("player-messages.inventory-drag", Map.of()));
        }
    }
}

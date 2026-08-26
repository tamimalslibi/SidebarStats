package net.yourserver.sidebarstats;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class StreakListener implements Listener {

    private final NamespacedKey currentStreakKey;
    private final NamespacedKey bestStreakKey;

    public StreakListener(SidebarStats plugin) {
        this.currentStreakKey = new NamespacedKey(plugin, "current_streak");
        this.bestStreakKey = new NamespacedKey(plugin, "best_streak");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        resetStreak(victim);

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            incrementStreak(killer);
        }
    }

    private void resetStreak(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(currentStreakKey, PersistentDataType.INTEGER, 0);
    }

    private void incrementStreak(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int current = pdc.getOrDefault(currentStreakKey, PersistentDataType.INTEGER, 0) + 1;
        pdc.set(currentStreakKey, PersistentDataType.INTEGER, current);

        int best = pdc.getOrDefault(bestStreakKey, PersistentDataType.INTEGER, 0);
        if (current > best) {
            pdc.set(bestStreakKey, PersistentDataType.INTEGER, current);
        }
    }

    public static int getBestStreak(Player player, SidebarStats plugin) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "best_streak");
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public static void resetBestStreak(Player player, SidebarStats plugin) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "best_streak"), PersistentDataType.INTEGER, 0);
        pdc.set(new NamespacedKey(plugin, "current_streak"), PersistentDataType.INTEGER, 0);
    }
}

package net.yourserver.sidebarstats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SidebarStats extends JavaPlugin {

    private TokenHook tokenHook;
    private ScoreboardTask scoreboardTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tokenHook = new TokenHook(this);
        this.tokenHook.setup();

        // Tracks kill streaks (best streak isn't a vanilla stat, so we track it ourselves)
        getServer().getPluginManager().registerEvents(new StreakListener(this), this);

        long interval = getConfig().getLong("update-interval-ticks", 20L);
        this.scoreboardTask = new ScoreboardTask(this, tokenHook);
        this.scoreboardTask.runTaskTimer(this, 20L, interval);

        getLogger().info("SidebarStats enabled.");
    }

    @Override
    public void onDisable() {
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sidebarstats.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            reloadConfig();
            tokenHook.setup();
            sender.sendMessage("§aSidebarStats config reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: /sidebarstats reload");
        return true;
    }
}

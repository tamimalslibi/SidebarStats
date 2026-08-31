package net.yourserver.sidebarstats;

import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SidebarStats extends JavaPlugin {

    private TokenHook tokenHook;
    private ScoreboardTask scoreboardTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tokenHook = new TokenHook(this);
        this.tokenHook.setup();

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
        if (args.length == 0) {
            sender.sendMessage("\u00A77Usage: /sidebarstats <reload|toggle|reset <player> [stat]|setstreak <player> <amount>>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("sidebarstats.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                reloadConfig();
                tokenHook.setup();
                sender.sendMessage("\u00A7aSidebarStats config reloaded.");
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("\u00A7cOnly players can use this.");
                    return true;
                }
                boolean nowHidden = scoreboardTask.toggle(player);
                sender.sendMessage(nowHidden
                        ? "\u00A77Scoreboard hidden. Run this again to show it."
                        : "\u00A7aScoreboard shown.");
            }
            case "reset" -> {
                if (!sender.hasPermission("sidebarstats.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("\u00A77Usage: /sidebarstats reset <player> [kills|deaths|streak|all]");
                    return true;
                }
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("\u00A7cThat player isn't online.");
                    return true;
                }
                String stat = args.length >= 3 ? args[2].toLowerCase() : "all";
                switch (stat) {
                    case "kills" -> {
                        target.setStatistic(Statistic.PLAYER_KILLS, 0);
                        sender.sendMessage("\u00A7aReset Kills for " + target.getName() + ".");
                    }
                    case "deaths" -> {
                        target.setStatistic(Statistic.DEATHS, 0);
                        sender.sendMessage("\u00A7aReset Deaths for " + target.getName() + ".");
                    }
                    case "streak" -> {
                        StreakListener.resetBestStreak(target, this);
                        sender.sendMessage("\u00A7aReset Best Streak for " + target.getName() + ".");
                    }
                    case "all" -> {
                        target.setStatistic(Statistic.PLAYER_KILLS, 0);
                        target.setStatistic(Statistic.DEATHS, 0);
                        StreakListener.resetBestStreak(target, this);
                        sender.sendMessage("\u00A7aReset Kills, Deaths, and Best Streak for " + target.getName() + ".");
                    }
                    default -> sender.sendMessage("\u00A77Usage: /sidebarstats reset <player> [kills|deaths|streak|all]");
                }
            }
            case "setstreak" -> {
                if (!sender.hasPermission("sidebarstats.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("\u00A77Usage: /sidebarstats setstreak <player> <amount>");
                    return true;
                }
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("\u00A7cThat player isn't online.");
                    return true;
                }
                int value;
                try {
                    value = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("\u00A7cThat's not a valid number.");
                    return true;
                }
                StreakListener.setBestStreak(target, this, value);
                sender.sendMessage("\u00A7aSet " + target.getName() + "'s Best Streak to " + value + ".");
            }
            default -> sender.sendMessage("\u00A77Usage: /sidebarstats <reload|toggle|reset <player> [stat]|setstreak <player> <amount>>");
        }
        return true;
    }
}

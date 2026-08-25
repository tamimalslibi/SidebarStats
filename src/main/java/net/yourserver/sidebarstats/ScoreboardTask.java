package net.yourserver.sidebarstats;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardTask extends BukkitRunnable {

    private final SidebarStats plugin;
    private final TokenHook tokenHook;
    private final Map<Player, Scoreboard> boards = new HashMap<>();

    public ScoreboardTask(SidebarStats plugin, TokenHook tokenHook) {
        this.plugin = plugin;
        this.tokenHook = tokenHook;
    }

    @Override
    public void run() {
        String title = plugin.getConfig().getString("title", "&b&lSTATS").replace('&', '\u00A7');

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Scoreboard board = boards.computeIfAbsent(player, p -> plugin.getServer().getScoreboardManager().getNewScoreboard());

            Objective objective = board.getObjective("sidebarstats");
            if (objective == null) {
                objective = board.registerNewObjective("sidebarstats", "dummy",
                        LegacyComponentSerializer.legacySection().deserialize(title));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else {
                objective.displayName(LegacyComponentSerializer.legacySection().deserialize(title));
            }

            // Clear old entries so removed/changed lines don't linger
            for (String entry : board.getEntries()) {
                board.resetScores(entry);
            }

            int kills = player.getStatistic(Statistic.PLAYER_KILLS);
            int deaths = player.getStatistic(Statistic.DEATHS);
            double kd = deaths == 0 ? kills : (double) kills / deaths;
            int bestStreak = StreakListener.getBestStreak(player, plugin);
            long tokens = tokenHook.getTokens(player);
            String tokensDisplay = tokens < 0 ? "N/A" : String.valueOf(tokens);
            String playtime = formatPlaytime(player.getStatistic(Statistic.PLAY_ONE_MINUTE));
            // Level tracked but not shown yet, per "Level (later)"
            int level = player.getLevel();

            String[] lines = new String[] {
                "\u00A77Kills: \u00A7f" + kills,
                "\u00A77Deaths: \u00A7f" + deaths,
                "\u00A77K/D: \u00A7f" + String.format("%.2f", kd),
                "\u00A77Best Streak: \u00A7f" + bestStreak,
                "\u00A77Tokens: \u00A7f" + tokensDisplay,
                "\u00A77Playtime: \u00A7f" + playtime
            };

            int score = lines.length;
            for (String line : lines) {
                objective.getScore(line).setScore(score--);
            }

            if (player.getScoreboard() != board) {
                player.setScoreboard(board);
            }
        }

        // Clean up entries for players who logged off
        boards.keySet().removeIf(p -> !p.isOnline());
    }

    private String formatPlaytime(int ticksStat) {
        long totalMinutes = ticksStat / 20L / 60L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }
}

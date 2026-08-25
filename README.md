# SidebarStats

A Paper 1.21.x plugin that shows a right-side scoreboard with:

```
Kills
Deaths
K/D
Best Streak
Tokens
Playtime
```

(Level is tracked internally already — `player.getLevel()` — so it's a
one-line addition to `ScoreboardTask.java` whenever you want it on the board.)

Kills, Deaths, and Playtime come from vanilla Minecraft's own stat tracking,
so they're accurate even for players who played before this plugin was
installed. Best Streak is tracked by this plugin (resets to 0 on death,
increments on a PvP kill, keeps the highest ever reached). Tokens is read
from Vault (if hooked) or, as a fallback, via reflection against the
DigitalTokens plugin.

## Building the jar

I can't reach the internet from this sandbox to download the Paper API and
compile it for you, so here are two ways to get an actual `.jar`:

### Option A — GitHub Actions (no local setup needed)
1. Create a new GitHub repo and push this whole folder to it.
2. The included `.github/workflows/build.yml` will build it automatically.
3. Go to the repo's **Actions** tab → the latest run → download the
   `SidebarStats` artifact. That's your jar.

### Option B — Build locally
Requires a JDK 21 and Maven installed.
```
mvn package
```
The compiled jar will be at `target/SidebarStats.jar`.

## Installing on PebbleHost

1. Stop the server (or use the file manager while it's running — Paper will
   just need a restart to load a new plugin).
2. Open your Pebble server's **File Manager** in the panel.
3. Go into the `plugins/` folder.
4. Upload `SidebarStats.jar` (drag-and-drop or the Upload button).
5. If you want Tokens via Vault, make sure `Vault.jar` is also in `plugins/`
   and that DigitalTokens registers with it.
6. Restart the server from the panel.
7. Check `logs/latest.log` for a line from `[SidebarStats]` — it'll tell you
   whether it resolved Tokens via Vault, via reflection, or couldn't find it
   at all.

## If Tokens shows N/A

The plugin tries several common method names against DigitalTokens
(`getTokens`, `getBalance`, `getPoints`, etc.) with a Player, OfflinePlayer,
UUID, or String argument. If none of those match DigitalTokens' real API,
it'll log a warning on first failure. Send me:
- the exact jar filename for DigitalTokens, and
- one real method signature from its docs/API jar (e.g.
  `public int getTokens(Player p)`)

and I'll swap the guesswork in `TokenHook.java` for a direct call.

## Config (`plugins/SidebarStats/config.yml`)

```yaml
update-interval-ticks: 20   # how often the board refreshes
title: "&b&lSTATS"
token-plugin-name: "DigitalTokens"
use-vault-first: true
```

Edit and run `/sidebarstats reload` (needs `sidebarstats.admin`, default op)
to apply changes without a restart.

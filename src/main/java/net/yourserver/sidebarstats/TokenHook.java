package net.yourserver.sidebarstats;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Reads a player's Tokens balance.
 *
 * Try order:
 *  1. Vault economy (works if DigitalTokens registers itself as a Vault
 *     economy provider — many token plugins do this automatically).
 *  2. Reflection against the DigitalTokens plugin instance, trying a list
 *     of common method names/signatures.
 *
 * If DigitalTokens has neither, this returns -1 and logs a one-time warning
 * telling you exactly what's missing. Send me the plugin's actual API (a
 * method signature from its docs/jar) and I'll hardcode the direct call
 * instead of guessing.
 */
public class TokenHook {

    private final SidebarStats plugin;
    private Economy vaultEconomy;
    private Plugin tokenPlugin;
    private Method resolvedMethod;
    private boolean warnedOnce = false;

    public TokenHook(SidebarStats plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        this.vaultEconomy = null;
        this.tokenPlugin = null;
        this.resolvedMethod = null;

        if (plugin.getConfig().getBoolean("use-vault-first", true)) {
            setupVault();
        }

        String tokenPluginName = plugin.getConfig().getString("token-plugin-name", "DigitalTokens");
        Plugin found = plugin.getServer().getPluginManager().getPlugin(tokenPluginName);
        if (found != null && found.isEnabled()) {
            this.tokenPlugin = found;
        }
    }

    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.vaultEconomy = rsp.getProvider();
        }
    }

    /**
     * Returns the player's token balance, or -1 if it couldn't be read.
     */
    public long getTokens(Player player) {
        if (vaultEconomy != null) {
            try {
                double bal = vaultEconomy.getBalance(player);
                return Math.round(bal);
            } catch (Exception ignored) {
                // fall through to reflection
            }
        }

        if (tokenPlugin != null) {
            Long viaReflection = tryReflection(player);
            if (viaReflection != null) {
                return viaReflection;
            }
        }

        if (!warnedOnce) {
            warnedOnce = true;
            plugin.getLogger().warning(
                "Couldn't read Tokens from Vault or from '" + plugin.getConfig().getString("token-plugin-name")
                + "'. Scoreboard will show N/A for Tokens. Send the plugin's real API method (from its "
                + "docs/jar) and it can be hardcoded instead of guessed."
            );
        }
        return -1;
    }

    private Long tryReflection(Player player) {
        if (resolvedMethod != null) {
            return invoke(resolvedMethod, player);
        }

        String[] candidateNames = {"getTokens", "getBalance", "getPoints", "tokens", "balance"};
        Class<?>[][] candidateParamSets = {
            {Player.class},
            {org.bukkit.OfflinePlayer.class},
            {java.util.UUID.class},
            {String.class}
        };

        for (String name : candidateNames) {
            for (Class<?>[] params : candidateParamSets) {
                try {
                    Method m = tokenPlugin.getClass().getMethod(name, params);
                    Object arg = params[0] == java.util.UUID.class ? player.getUniqueId()
                               : params[0] == String.class ? player.getName()
                               : player;
                    Long result = invoke(m, arg);
                    if (result != null) {
                        resolvedMethod = m;
                        plugin.getLogger().info("SidebarStats: resolved Tokens via "
                            + tokenPlugin.getName() + "#" + name + "(" + params[0].getSimpleName() + ")");
                        return result;
                    }
                } catch (NoSuchMethodException ignored) {
                    // try next candidate
                }
            }
        }
        return null;
    }

    private Long invoke(Method m, Object arg) {
        try {
            m.setAccessible(true);
            Object result = m.invoke(tokenPlugin, arg);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            if (result instanceof java.util.Optional<?> opt && opt.isPresent() && opt.get() instanceof Number n) {
                return n.longValue();
            }
        } catch (Exception ignored) {
            // this candidate didn't work, caller tries the next one
        }
        return null;
    }
}

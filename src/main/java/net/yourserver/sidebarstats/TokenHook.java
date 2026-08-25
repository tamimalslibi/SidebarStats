package net.yourserver.sidebarstats;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;

public class TokenHook {

    private final SidebarStats plugin;
    private Economy vaultEconomy;
    private Plugin tokenPlugin;
    private File tokenDataFile;
    private YamlConfiguration cachedConfig;
    private long cachedLastModified = -1L;
    private boolean warnedOnce = false;

    public TokenHook(SidebarStats plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        this.vaultEconomy = null;
        this.tokenPlugin = null;
        this.tokenDataFile = null;
        this.cachedConfig = null;
        this.cachedLastModified = -1L;

        if (plugin.getConfig().getBoolean("use-vault-first", true)) {
            setupVault();
        }

        String tokenPluginName = plugin.getConfig().getString("token-plugin-name", "DigitalTokens");
        Plugin found = plugin.getServer().getPluginManager().getPlugin(tokenPluginName);
        if (found != null && found.isEnabled()) {
            this.tokenPlugin = found;
            File candidate = new File(found.getDataFolder(), "data.yml");
            if (candidate.exists()) {
                this.tokenDataFile = candidate;
                plugin.getLogger().info("SidebarStats: found token data file at " + candidate.getPath());
            }
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

    public long getTokens(Player player) {
        if (vaultEconomy != null) {
            try {
                double bal = vaultEconomy.getBalance(player);
                return Math.round(bal);
            } catch (Exception ignored) {
            }
        }

        Long viaFile = tryDirectFile(player);
        if (viaFile != null) {
            return viaFile;
        }

        Long viaReflection = tryReflection(player);
        if (viaReflection != null) {
            return viaReflection;
        }

        if (!warnedOnce) {
            warnedOnce = true;
            plugin.getLogger().warning(
                "Couldn't read Tokens from Vault, data.yml, or reflection against '"
                + plugin.getConfig().getString("token-plugin-name") + "'. Showing N/A for Tokens."
            );
        }
        return -1;
    }

    private Long tryDirectFile(Player player) {
        if (tokenDataFile == null || !tokenDataFile.exists()) {
            return null;
        }
        try {
            long lastModified = tokenDataFile.lastModified();
            if (cachedConfig == null || lastModified != cachedLastModified) {
                cachedConfig = YamlConfiguration.loadConfiguration(tokenDataFile);
                cachedLastModified = lastModified;
            }
            String key = player.getUniqueId().toString();
            if (cachedConfig.contains(key)) {
                return cachedConfig.getLong(key);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Long tryReflection(Player player) {
        if (tokenPlugin == null) {
            return null;
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
                    java.lang.reflect.Method m = tokenPlugin.getClass().getMethod(name, params);
                    Object arg = params[0] == java.util.UUID.class ? player.getUniqueId()
                               : params[0] == String.class ? player.getName()
                               : player;
                    m.setAccessible(true);
                    Object result = m.invoke(tokenPlugin, arg);
                    if (result instanceof Number number) {
                        return number.longValue();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}

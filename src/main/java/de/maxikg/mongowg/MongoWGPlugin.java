package de.maxikg.mongowg;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.maxikg.mongowg.utils.InjectionUtils;
import de.maxikg.mongowg.wg.storage.MongoRegionDriver;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin.
 */
public class MongoWGPlugin extends JavaPlugin {

    private MongoClient client;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();

        client = MongoClients.create(getConfig().getString("mongodb.uri"));
        MongoDatabase database = client.getDatabase(getConfig().getString("mongodb.database"));
        MongoRegionDriver driver = new MongoRegionDriver(getServer(), database);

        WorldGuardPlugin wgPlugin = WorldGuardPlugin.inst();
        ConfigurationManager config = wgPlugin.getGlobalStateManager();
        RegionContainer container = wgPlugin.getRegionContainer();
        InjectionUtils.injectRegionDriver(container, driver);
        InjectionUtils.callUnload(container);
        InjectionUtils.callLoadWorlds(container);
        config.selectedRegionStoreDriver = driver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisable() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}

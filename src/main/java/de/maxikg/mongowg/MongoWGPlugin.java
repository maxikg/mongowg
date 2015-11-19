package de.maxikg.mongowg;

import com.mongodb.ConnectionString;
import com.mongodb.MongoTimeoutException;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.maxikg.mongowg.codec.BlockVector2DCodec;
import de.maxikg.mongowg.codec.BlockVectorCodec;
import de.maxikg.mongowg.codec.DefaultDomainCodec;
import de.maxikg.mongowg.codec.ProcessingProtectedRegionCodec;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import de.maxikg.mongowg.oplog.OpLogParser;
import de.maxikg.mongowg.oplog.OpLogRetriever;
import de.maxikg.mongowg.utils.InjectionUtils;
import de.maxikg.mongowg.utils.OpLogUtils;
import de.maxikg.mongowg.utils.OperationResultCallback;
import de.maxikg.mongowg.wg.storage.MongoRegionDriver;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

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

        CodecRegistry codecRegistry = createCodecRegistry();
        MongoClientSettings settings = MongoClientSettings.builder()
                .clusterSettings(ClusterSettings.builder().applyConnectionString(new ConnectionString(getConfig().getString("mongodb.uri"))).build())
                .codecRegistry(codecRegistry)
                .build();
        client = MongoClients.create(settings);
        MongoDatabase database = client.getDatabase(getConfig().getString("mongodb.database"));
        if (!testConnection(database))
            return;
        RegionStorageAdapter storageAdapter = new RegionStorageAdapter(database);
        MongoRegionDriver driver = new MongoRegionDriver(getServer(), storageAdapter);

        WorldGuardPlugin wgPlugin = WorldGuardPlugin.inst();
        if (getConfig().getBoolean("mongodb.use_oplog")) {
            getLogger().info("OpLog usage enabled.");
            getServer().getScheduler().runTaskAsynchronously(this, new OpLogRetriever(
                    OpLogUtils.getCollection(client),
                    new OpLogParser(new WorldGuardOpLogHandler(codecRegistry.get(ProcessingProtectedRegion.class), storageAdapter, wgPlugin)),
                    getConfig().getString("mongodb.database") + "." + RegionStorageAdapter.COLLECTION_NAME
            ));
        }

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

    private boolean testConnection(MongoDatabase database) {
        CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        boolean erroneous = false;
        try {
            database.getCollection(RegionStorageAdapter.COLLECTION_NAME).count(new OperationResultCallback<Long>(error, waiter));
            waiter.await();
            Throwable realError = error.get();
            if (realError != null)
                throw realError;
        } catch (MongoTimeoutException ignore) {
            getLogger().severe("Cannot connect to MongoDB server.");
            erroneous = true;
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "An error occurred while connecting to database.", throwable);
            erroneous = true;
        }

        if (erroneous) {
            getLogger().severe("An error was encountered. Disabling plugin and NOT injecting into WorldGuard.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        return true;
    }

    private static CodecRegistry createCodecRegistry() {
        CodecRegistry common = CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(new ValueCodecProvider(), new DocumentCodecProvider(), new BsonValueCodecProvider()),
                CodecRegistries.fromCodecs(new BlockVector2DCodec(), new BlockVectorCodec(), new DefaultDomainCodec())
        );
        return CodecRegistries.fromRegistries(
                common,
                CodecRegistries.fromCodecs(new ProcessingProtectedRegionCodec(common))
        );
    }
}

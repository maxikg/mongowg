package de.maxikg.mongowg.utils;

import com.google.common.base.Throwables;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Some utilities to deal with MongoDB's oplog.
 */
public class OpLogUtils {

    public static final String OPLOG_COLLECTION = "oplog.$main";
    public static final String OPLOG_DATABASE = "local";

    private OpLogUtils() {
    }

    /**
     * Returns the timestamp of the latest oplog entry.
     *
     * @param collection The oplog {@link MongoCollection}
     * @return The latest timestamp or {@code null} if no entry is available
     */
    public static BsonTimestamp getLatestOplogTimestamp(MongoCollection<BsonDocument> collection) {
        final AtomicReference<BsonTimestamp> timestamp = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(1);
        collection.find().sort(new Document("$natural", -1)).limit(1).first(new SingleResultCallback<BsonDocument>() {
            @Override
            public void onResult(BsonDocument document, Throwable throwable) {
                if (throwable != null)
                    error.set(throwable);
                if (document != null)
                    timestamp.set(document.getTimestamp("ts"));
                waiter.countDown();
            }
        });
        ConcurrentUtils.safeAwait(waiter);
        Throwable realError = error.get();
        if (realError != null)
            throw Throwables.propagate(realError);
        return timestamp.get();
    }

    /**
     * Returns the {@code MongoCollection} which contains the oplog.
     *
     * @param client The {@link MongoClient}
     * @return The {@code MongoCollection} which contains the oplog
     */
    public static MongoCollection<BsonDocument> getCollection(MongoClient client) {
        return client.getDatabase(OPLOG_DATABASE).getCollection(OPLOG_COLLECTION, BsonDocument.class);
    }
}

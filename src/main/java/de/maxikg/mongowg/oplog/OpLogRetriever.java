package de.maxikg.mongowg.oplog;

import com.google.common.base.Preconditions;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.maxikg.mongowg.utils.ConcurrentUtils;
import de.maxikg.mongowg.utils.OpLogUtils;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Runnable} which can be used to obtain oplog information.
 */
public class OpLogRetriever implements Runnable {

    private final MongoCollection<BsonDocument> oplog;
    private final OpLogParser parser;
    private final String namespace;

    /**
     * Constructor.
     *
     * @param oplog The oplog collection
     * @param parser An instance of {@link OpLogParser}
     * @param namespace The namespace for which should be listened
     */
    public OpLogRetriever(MongoCollection<BsonDocument> oplog, OpLogParser parser, String namespace) {
        this.oplog = Preconditions.checkNotNull(oplog, "oplog must be not null.");
        this.parser = Preconditions.checkNotNull(parser, "parser must be not null.");
        this.namespace = Preconditions.checkNotNull(namespace, "namespace must be not null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        final AtomicReference<BsonTimestamp> last = new AtomicReference<>(OpLogUtils.getLatestOplogTimestamp(oplog));
        //noinspection InfiniteLoopStatement
        while (true) {
            final CountDownLatch waiter = new CountDownLatch(1);
            oplog.find(Filters.and(Filters.gt("ts", last.get()), Filters.eq("ns", namespace))).cursorType(CursorType.TailableAwait).forEach(
                    new Block<BsonDocument>() {
                        @Override
                        public void apply(BsonDocument document) {
                            BsonTimestamp current = document.getTimestamp("ts");
                            if (current.getTime() > last.get().getTime()) {
                                last.set(current);
                                parser.emit(document);
                            }
                        }
                    },
                    new SingleResultCallback<Void>() {
                        @Override
                        public void onResult(Void aVoid, Throwable throwable) {
                            waiter.countDown();
                        }
                    }
            );
            ConcurrentUtils.safeAwait(waiter);
        }
    }
}

package de.maxikg.mongowg.oplog;

import com.google.common.base.Preconditions;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.maxikg.mongowg.utils.ConcurrentUtils;
import de.maxikg.mongowg.utils.OpLogUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class OpLogRetriever implements Runnable {

    private final MongoCollection<Document> oplog;
    private final OpLogParser parser;
    private final String namespace;

    public OpLogRetriever(MongoCollection<Document> oplog, OpLogParser parser, String namespace) {
        this.oplog = Preconditions.checkNotNull(oplog, "oplog must be not null.");
        this.parser = Preconditions.checkNotNull(parser, "parser must be not null.");
        this.namespace = Preconditions.checkNotNull(namespace, "namespace must be not null.");
    }

    @Override
    public void run() {
        final AtomicReference<BsonTimestamp> last = new AtomicReference<>(OpLogUtils.getLatestOplogTimestamp(oplog));
        //noinspection InfiniteLoopStatement
        while (true) {
            final CountDownLatch waiter = new CountDownLatch(1);
            oplog.find(Filters.and(Filters.gt("ts", last.get()), Filters.eq("ns", namespace))).cursorType(CursorType.TailableAwait).forEach(
                    new Block<Document>() {
                        @Override
                        public void apply(Document document) {
                            BsonTimestamp current = document.get("ts", BsonTimestamp.class);
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

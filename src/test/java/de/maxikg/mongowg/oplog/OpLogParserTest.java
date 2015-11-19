package de.maxikg.mongowg.oplog;

import de.maxikg.mongowg.utils.ConcurrentUtils;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class OpLogParserTest {

    private static final CodecRegistry REGISTRY = CodecRegistries.fromProviders(new ValueCodecProvider(), new DocumentCodecProvider(), new BsonValueCodecProvider());
    private static final String ID = "563b49015fc8c2cb93d4a443";
    private static final String NAMESPACE = "test_database.test_collection";
    private static final int SECONDS = (int)(System.currentTimeMillis() / 2);
    private static final BsonDocument CREATE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "i")
            .append("ns", NAMESPACE)
            .append("o", new Document("_id", new ObjectId(ID)).append("hello", "world"))
            .toBsonDocument(BsonDocument.class, REGISTRY);
    private static final BsonDocument UPDATE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "u")
            .append("ns", NAMESPACE)
            .append("o2", new Document("_id", new ObjectId(ID)))
            .append("o", new Document("_id", new ObjectId(ID)).append("hello", "junit"))
            .toBsonDocument(BsonDocument.class, REGISTRY);
    private static final BsonDocument DELETE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "d")
            .append("ns", NAMESPACE)
            .append("b", true)
            .append("o", new Document("_id", new ObjectId(ID)))
            .toBsonDocument(BsonDocument.class, REGISTRY);

    @Test
    public void testParseCreate() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<BsonDocument> document = new AtomicReference<>();

        new OpLogParser(new TestOpLogHandler(error, waiter) {
            @Override
            public void onCreate(BsonDocument createdDocument) {
                document.set(createdDocument);
                waiter.countDown();
            }
        }).emit(CREATE);
        ConcurrentUtils.safeAwait(waiter);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        BsonDocument realDocument = document.get();
        Assert.assertEquals(ID, realDocument.getObjectId("_id").getValue().toHexString());
        Assert.assertEquals("world", realDocument.getString("hello").getValue());
    }

    @Test
    public void testParseUpdate() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<ObjectId> id = new AtomicReference<>();

        new OpLogParser(new TestOpLogHandler(error, waiter) {
            @Override
            public void onUpdate(ObjectId updatedObject) {
                id.set(updatedObject);
                waiter.countDown();
            }
        }).emit(UPDATE);
        ConcurrentUtils.safeAwait(waiter);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        Assert.assertEquals(ID, id.get().toHexString());
    }

    @Test
    public void testParseDelete() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<ObjectId> id = new AtomicReference<>();

        new OpLogParser(new TestOpLogHandler(error, waiter) {
            @Override
            public void onDelete(ObjectId deletedObject) {
                id.set(deletedObject);
                waiter.countDown();
            }
        }).emit(DELETE);
        ConcurrentUtils.safeAwait(waiter);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        Assert.assertEquals(ID, id.get().toHexString());
    }

    private static abstract class TestOpLogHandler implements OpLogHandler {

        private final AtomicReference<Throwable> error;
        private final CountDownLatch waiter;

        public TestOpLogHandler(AtomicReference<Throwable> error, CountDownLatch waiter) {
            this.error = error;
            this.waiter = waiter;
        }

        @Override
        public void onCreate(BsonDocument createdDocument) {
            error.set(new UnsupportedOperationException("Doesn't expect insert."));
            waiter.countDown();
        }

        @Override
        public void onUpdate(ObjectId updatedDocument) {
            error.set(new UnsupportedOperationException("Doesn't expect update."));
            waiter.countDown();
        }

        @Override
        public void onDelete(ObjectId deletedObject) {
            error.set(new UnsupportedOperationException("Doesn't expect removal."));
            waiter.countDown();
        }

        @Override
        public void onException(Throwable throwable) {
            error.set(throwable);
            waiter.countDown();
        }
    }
}

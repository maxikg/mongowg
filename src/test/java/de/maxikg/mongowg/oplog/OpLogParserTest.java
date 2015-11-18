package de.maxikg.mongowg.oplog;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class OpLogParserTest {

    private static final String ID = "563b49015fc8c2cb93d4a443";
    private static final String NAMESPACE = "test_database.test_collection";
    private static final int SECONDS = (int)(System.currentTimeMillis() / 2);
    private static final Document CREATE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "i")
            .append("ns", NAMESPACE)
            .append("o", new Document("_id", new ObjectId(ID)).append("hello", "world"));
    private static final Document UPDATE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "u")
            .append("ns", NAMESPACE)
            .append("o2", new Document("_id", new ObjectId(ID)))
            .append("o", new Document("_id", new ObjectId(ID)).append("hello", "junit"));
    private static final Document DELETE = new Document()
            .append("ts", new BsonTimestamp(SECONDS, 0))
            .append("op", "d")
            .append("ns", NAMESPACE)
            .append("b", true)
            .append("o", new Document("_id", new ObjectId(ID)));

    @Test
    public void testParseCreate() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<Document> document = new AtomicReference<>();

        new OpLogParser(new OpLogHandler() {
            @Override
            public void onCreate(Document createdDocument) {
                document.set(createdDocument);
                waiter.countDown();
            }

            @Override
            public void onUpdate(Document updatedDocument) {
                error.set(new UnsupportedOperationException("Doesn't expect update."));
                waiter.countDown();
            }

            @Override
            public void onDelete(ObjectId deletedObject) {
                error.set(new UnsupportedOperationException("Doesn't expect delete."));
                waiter.countDown();
            }

            @Override
            public void onException(Throwable throwable) {
                error.set(throwable);
                waiter.countDown();
            }
        }).emit(CREATE);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        Document realDocument = document.get();
        Assert.assertEquals(ID, realDocument.getObjectId("_id").toHexString());
        Assert.assertEquals("world", realDocument.getString("hello"));
    }

    @Test
    public void testParseUpdate() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<Document> document = new AtomicReference<>();

        new OpLogParser(new OpLogHandler() {
            @Override
            public void onCreate(Document createdDocument) {
                error.set(new UnsupportedOperationException("Doesn't expect insert."));
                waiter.countDown();
            }

            @Override
            public void onUpdate(Document updatedDocument) {
                document.set(updatedDocument);
                waiter.countDown();
            }

            @Override
            public void onDelete(ObjectId deletedObject) {
                error.set(new UnsupportedOperationException("Doesn't expect delete."));
                waiter.countDown();
            }

            @Override
            public void onException(Throwable throwable) {
                error.set(throwable);
                waiter.countDown();
            }
        }).emit(UPDATE);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        Document realDocument = document.get();
        Assert.assertEquals(ID, realDocument.getObjectId("_id").toHexString());
        Assert.assertEquals("junit", realDocument.getString("hello"));
    }

    @Test
    public void testParseDelete() throws Throwable {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<ObjectId> id = new AtomicReference<>();

        new OpLogParser(new OpLogHandler() {
            @Override
            public void onCreate(Document createdDocument) {
                error.set(new UnsupportedOperationException("Doesn't expect insert."));
                waiter.countDown();
            }

            @Override
            public void onUpdate(Document updatedDocument) {
                error.set(new UnsupportedOperationException("Doesn't expect update."));
                waiter.countDown();
            }

            @Override
            public void onDelete(ObjectId deletedObject) {
                id.set(deletedObject);
                waiter.countDown();
            }

            @Override
            public void onException(Throwable throwable) {
                error.set(throwable);
                waiter.countDown();
            }
        }).emit(DELETE);

        Throwable realError = error.get();
        if (realError != null)
            throw realError;

        Assert.assertEquals(ID, id.get().toHexString());
    }
}

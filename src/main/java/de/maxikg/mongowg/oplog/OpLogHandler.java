package de.maxikg.mongowg.oplog;

import org.bson.Document;
import org.bson.types.ObjectId;

public interface OpLogHandler {

    void onCreate(Document createdDocument);

    void onUpdate(Document updatedDocument);

    void onDelete(ObjectId deletedObject);

    void onException(Throwable throwable);
}

package de.maxikg.mongowg.oplog;

import org.bson.BsonDocument;
import org.bson.types.ObjectId;

public interface OpLogHandler {

    void onCreate(BsonDocument createdDocument);

    void onUpdate(ObjectId updatedObject);

    void onDelete(ObjectId deletedObject);

    void onException(Throwable throwable);
}

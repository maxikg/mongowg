package de.maxikg.mongowg.oplog;

import org.bson.BsonDocument;
import org.bson.types.ObjectId;

/**
 * Listener for OpLog events.
 */
public interface OpLogHandler {

    /**
     * Called on document creation.
     *
     * @param createdDocument The whole created document as {@link BsonDocument}
     */
    void onCreate(BsonDocument createdDocument);

    /**
     * Called on document update.
     *
     * @param updatedObject The {@link ObjectId} of the updated document
     */
    void onUpdate(ObjectId updatedObject);

    /**
     * Called on document deletion.
     *
     * @param deletedObject The {@link ObjectId} of the deleted document
     */
    void onDelete(ObjectId deletedObject);

    /**
     * Called on a exception.
     */
    void onException(Throwable throwable);
}

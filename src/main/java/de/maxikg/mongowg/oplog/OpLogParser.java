package de.maxikg.mongowg.oplog;

import com.google.common.base.Preconditions;
import org.bson.BsonDocument;

public class OpLogParser {

    private static final String OP_CREATE = "i";
    private static final String OP_UPDATE = "u";
    private static final String OP_DELETE = "d";

    private final OpLogHandler handler;

    public OpLogParser(OpLogHandler handler) {
        this.handler = Preconditions.checkNotNull(handler, "handler must be not null.");
    }

    public void emit(BsonDocument document) {
        String action = document.getString("op").getValue();
        try {
            if (OP_CREATE.equals(action))
                handler.onCreate(document.getDocument("o"));
            else if (OP_UPDATE.equals(action))
                handler.onUpdate(document.getDocument("o2").getObjectId("_id").getValue());
            else if (OP_DELETE.equals(action))
                handler.onDelete(document.getDocument("o").getObjectId("_id").getValue());
        } catch (Throwable e) {
            handler.onException(e);
        }
    }
}

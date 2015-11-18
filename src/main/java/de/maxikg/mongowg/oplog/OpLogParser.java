package de.maxikg.mongowg.oplog;

import com.google.common.base.Preconditions;
import org.bson.Document;

public class OpLogParser {

    private static final String OP_CREATE = "i";
    private static final String OP_UPDATE = "u";
    private static final String OP_DELETE = "d";

    private final OpLogHandler handler;

    public OpLogParser(OpLogHandler handler) {
        this.handler = Preconditions.checkNotNull(handler, "handler must be not null.");
    }

    public void emit(Document document) {
        String action = document.getString("op");
        try {
            if (OP_CREATE.equals(action))
                handler.onCreate(document.get("o", Document.class));
            else if (OP_UPDATE.equals(action))
                handler.onUpdate(document.get("o", Document.class));
            else if (OP_DELETE.equals(action))
                handler.onDelete(document.get("o", Document.class).getObjectId("_id"));
        } catch (Throwable e) {
            handler.onException(e);
        }
    }
}

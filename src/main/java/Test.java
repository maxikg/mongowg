import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import de.maxikg.mongowg.oplog.OpLogHandler;
import de.maxikg.mongowg.oplog.OpLogParser;
import de.maxikg.mongowg.oplog.OpLogRetriever;
import de.maxikg.mongowg.utils.OpLogUtils;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;

public class Test {

    public static void main(String[] args) {
        OpLogParser parser = new OpLogParser(new OpLogHandler() {
            @Override
            public void onCreate(BsonDocument createdDocument) {
                System.out.println("Created: " + createdDocument);
            }

            @Override
            public void onUpdate(ObjectId updatedDocument) {
                System.out.println("Updated: " + updatedDocument);
            }

            @Override
            public void onDelete(ObjectId deletedObject) {
                System.out.println("Deleted: " + deletedObject);
            }

            @Override
            public void onException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        MongoClient client = MongoClients.create();
        new Thread(new OpLogRetriever(OpLogUtils.getCollection(client), parser, "mongowg.regions")).start();
    }
}

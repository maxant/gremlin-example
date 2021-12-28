package ch.maxant.janusgraph;

import com.sleepycat.je.*;
import com.sleepycat.persist.StoreConfig;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.joining;
import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

public class App {
    public static void main( String[] args ) throws Exception {

        try(GraphTraversalSource g = traversal().withRemote("conf/remote-graph.properties")) {
        //Graph g = null;
        //try(GraphTraversalSource g = traversal().withEmbedded(g)) {

            g.V().outE().drop().iterate();
            System.out.println("dropped all edges");
            g.V().drop().iterate();
            System.out.println("dropped all verticies");

            doInTransaction(g, () -> {
                // https://tinkerpop.apache.org/docs/current/tutorials/getting-started/
                Vertex v1 = g.addV("person").property("name", "ant").property("uid", UUID.randomUUID()).next();
                System.out.println("added ant: " + v1.id());
                Vertex v2 = g.addV("person").property("name", "clare").property("uid", UUID.randomUUID()).next();
                System.out.println("added clare: " + v2.id());
                Edge e = g.addE("marriedTo").from(v1).to(v2).next();
                System.out.println("added edge: " + e.id());
                return null;
            });

            System.out.println("written...");

            g.V().forEachRemaining(v -> System.out.println("got a vertext: " + v));

            List<?> c = g.V() // find verticies
                    .has("name", "clare") // with this property
                    .both("marriedTo") // map to verticies on the other side of this edge, ignoring direction using both (otherwise use out/in)
                    .valueMap().with(WithOptions.tokens) // get the properties, including the label (vertex type) and internal id
                    //.values("name", "uid") // get just the properties values, specifically the props "name" and "uid"
                    .toList(); // get the results
            System.out.println("clare's married to " +
                    c.stream().map(v->v.toString()).collect(joining(",")) // valueMap returns a map with just those queried keys, not extract them
            );

            System.out.println("reading berkley db:");


            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setReadOnly(true);
            envConfig.setAllowCreate(false);

            try {
                Environment myDbEnvironment_ = new Environment(new File("/shared2/janusgraph/data/"), envConfig);
                StoreConfig config = new StoreConfig();
                config.setReadOnly(true);
                config.setAllowCreate(false);
                DatabaseConfig dbConfig = new DatabaseConfig();
                dbConfig.setReadOnly(true);
                dbConfig.setAllowCreate(false);
                System.out.println(myDbEnvironment_.getDatabaseNames());
                // Database db = myDbEnvironment_.openDatabase(null, "janusgraph_ids", dbConfig);
                // Database db = myDbEnvironment_.openDatabase(null, "system_properties", dbConfig);
                // Database db = myDbEnvironment_.openDatabase(null, "edgestore", dbConfig);
                Database db = myDbEnvironment_.openDatabase(null, "graphindex", dbConfig);

                //Transaction txn = myDbEnvironment_.beginTransaction(null, null);
                DatabaseEntry key = new DatabaseEntry();
                DatabaseEntry data = new DatabaseEntry();
                long startTime = System.currentTimeMillis();
                int recordsVisited = 0;

                Cursor cursor = db.openCursor(/*txn*/null, null);
                while (cursor.getNext(key, data, LockMode.DEFAULT) ==
                        OperationStatus.SUCCESS) {
                    byte[] dataBytes = data.getData();
                    assert dataBytes.length == 1024;
                    System.out.println(recordsVisited + ": '" + new String(key.getData()) + "' -> '" + new String(dataBytes) + "'");
                    recordsVisited++;
                }
                cursor.close();
                //txn.commit();

                long endTime = System.currentTimeMillis();
                System.out.println("Retrieved " + recordsVisited + " 1k records in " +
                        (endTime - startTime) + " milliseconds");

                db.close();
                myDbEnvironment_.close();

            } catch (DatabaseException e) {
                e.printStackTrace();
            }
/*
            // Class.forName("")
            try(Connection conn = DriverManager.getConnection("jdbc:sqlite:/shared2/janusgraph/data/")) {
                try(ResultSet rs = conn.getMetaData().getTables(null, null, null, null)) {
                    while(rs.next()) {
                        for(int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            System.out.print(rs.getMetaData().getColumnName(i) + "=" + rs.getString(i));
                        }
                    }
                }
            }
*/
            System.out.println("done");
        }
    }

    private static <T> T doInTransaction(GraphTraversalSource g, Callable<T> c) throws Exception {
//        Transaction tx = g.getGraph().tx(); TODO Graph does not support transactions
        try {
//            tx.begin();
            T t = c.call();
//            tx.commit();
            return t;
        }catch (Exception e) {
//            tx.rollback();
            throw e;
        }
    }
}

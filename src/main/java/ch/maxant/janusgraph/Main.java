package ch.maxant.janusgraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

public class Main {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\<a.*href\\s*\\=\\s*[\"'](?<url>[^\"']*)['\"].*\\>");

    public static void main(String[] args) throws Exception {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try (GraphTraversalSource g = traversal().withRemote("conf/remote-graph.properties")) {

            // https://www.baeldung.com/java-9-http-client
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://en.wikipedia.org/wiki/Special:Random"))
                    .timeout(Duration.of(10, SECONDS))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); // TODO sendAsync
            System.out.println("on page" + response.uri());
            if(response.statusCode() < 200 || response.statusCode() > 299) {
                throw new RuntimeException("response was: " + response.statusCode() + " with body " + response.body());
            }

            String url = response.uri().toString();
            GraphTraversal<Vertex, Vertex> currentPage = g.V().has("url", url);
            Vertex page;
            if(currentPage.hasNext()) {
                page = currentPage.next();
                System.out.println("page exists: " + url);
            } else {
                page = g.addV("page").property("url", url).property("uid", UUID.randomUUID()).next();
                System.out.println("added page " + url);
            }

            Matcher matcher = LINK_PATTERN.matcher(response.body());
            while(matcher.find()) {
                url = matcher.group("url");
                if(!url.startsWith("#") && !url.contains("action=edit") && !url.contains("action=history") && !url.contains("printable=yes") && !url.contains("Special:")) {
                    // TODO normalise url, ie add domain if missing
                    GraphTraversal<Vertex, Vertex> targetPage = g.V().has("url", url);
                    Vertex target;
                    if(targetPage.hasNext()) {
                        target = targetPage.next();
                        System.out.println("target exists: " + url);
                    } else {
                        target = g.addV("page").property("url", url).property("uid", UUID.randomUUID()).next();
                        System.out.println("added target: " + target.id() + " -> " + url);
                    }

                    GraphTraversal<Vertex, Vertex> existingTarget = g.V(page.id()).out("pointsTo").hasId(target.id());
                    if(!existingTarget.hasNext()) {
                        Edge newEdge = g.addE("pointsTo").from(page).to(target).property("createdAt", now).next();
                        System.out.println("added edge: " + newEdge.id() + " -> " + url);
                    } else {
                        System.out.println("edge already exists to " + url);
                    }
                }
            }

if(false) {
    g.V().outE().drop().iterate();
    System.out.println("dropped all edges");
    g.V().drop().iterate();
    System.out.println("dropped all verticies");

    // https://tinkerpop.apache.org/docs/current/tutorials/getting-started/
    Vertex v1 = g.addV("person").property("name", "ant").property("uid", UUID.randomUUID()).next();
    System.out.println("added ant: " + v1.id());
    Vertex v2 = g.addV("person").property("name", "clare").property("uid", UUID.randomUUID()).next();
    System.out.println("added clare: " + v2.id());
    Edge e = g.addE("marriedTo").from(v1).to(v2).next();
    System.out.println("added edge: " + e.id());

    System.out.println("written...");

    g.V().forEachRemaining(v -> System.out.println("got a vertext: " + v));

    List<?> c = g.V() // find verticies
            .has("name", "clare") // with this property
            .both("marriedTo") // map to verticies on the other side of this edge, ignoring direction using both (otherwise use out/in)
            .valueMap().with(WithOptions.tokens) // get the properties, including the label (vertex type) and internal id
            //.values("name", "uid") // get just the properties values, specifically the props "name" and "uid"
            .toList(); // get the results
    System.out.println("clare's married to " +
            c.stream().map(v -> v.toString()).collect(joining(",")) // valueMap returns a map with just those queried keys, not extract them
    );
}
            System.out.println("done");
        }
    }

}

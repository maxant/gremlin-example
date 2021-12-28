# setup

see https://github.com/JanusGraph/janusgraph-docker/blob/master/README.md

make data and index folders:

    mkdir /shared2/janusgraph/data
    cd /shared2/janusgraph
    chmod -R a+rwx data/
    mkdir /shared2/janusgraph/index
    chmod -R a+rwx index/

start the server with berkleydb and lucene as defaults:

    docker run --name janusgraph --rm -v /shared2/janusgraph/data:/var/lib/janusgraph/data -v /shared2/janusgraph/index:/var/lib/janusgraph/index -p:8182:8182 docker.io/janusgraph/janusgraph:0.6.0


Inspect the configuration:

    docker exec janusgraph sh -c 'cat /etc/opt/janusgraph/janusgraph.properties | grep ^[a-z]'
    docker exec janusgraph cat /etc/opt/janusgraph/janusgraph-server.yaml

connect to this "remote" server:

    docker run --rm --link janusgraph:janusgraph -e GREMLIN_REMOTE_HOSTS=janusgraph -it janusgraph/janusgraph:0.6.0 ./bin/gremlin.sh
    :remote connect tinkerpop.server conf/remote.yaml
    :> g.V()

setup java 17 and check maven version:

    export PATH=/shared2/graalvm/graalvm-ce-java17-21.3.0/bin/:$PATH
    export JAVA_HOME=/shared2/graalvm/graalvm-ce-java17-21.3.0/
    mvn --version

create a maven project:

    mvn archetype:generate -DgroupId=ch.maxant.janusgraph -DartifactId=gremlin-example -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

https://github.com/opencypher/cypher-for-gremlin: not used yet, coz it onyl seems to be tested agains janusgraph 0.4.0 and not the version used here (0.6.0)

visualisation: https://github.com/bricaud/graphexp => clone this, change server url in graphexp.html

    cd graphexp
    http-server

open browser at: http://192.168.1.151:8080/graphexp.html

enter "person" in the `node label` field and click search


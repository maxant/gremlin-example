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

setup java 17 and check maven version:

    export PATH=/shared2/graalvm/graalvm-ce-java17-21.3.0/bin/:$PATH
    export JAVA_HOME=/shared2/graalvm/graalvm-ce-java17-21.3.0/
    mvn --version

create a maven project:

    mvn archetype:generate -DgroupId=ch.maxant.janusgraph -DartifactId=gremlin-example -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false


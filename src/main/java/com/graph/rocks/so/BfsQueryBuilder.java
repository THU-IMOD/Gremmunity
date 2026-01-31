package com.graph.rocks.so;

import com.graph.rocks.RustJNI;
import com.graph.rocks.community.CommunityGraph;
import com.graph.rocks.community.CommunityVertex;
import com.graph.rocks.serialize.VsetResultSerializer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.*;

public class BfsQueryBuilder {
    private final GraphTraversalSource g;
    private final Vertex vertex;

    /**
     * Create a new SecondOrderQueryBuilder instance
     * @param g GraphTraversalSource for executing Gremlin queries
     */
    public BfsQueryBuilder(GraphTraversalSource g, Object id) {
        this.g = g;
        this.vertex = g.V(id).next();
    }

    /**
     * Static factory method to initialize second-order query builder
     * @param g GraphTraversalSource for query execution
     * @return New SecondOrderQueryBuilder instance
     */
    public static SecondOrderQueryBuilder secondOrder(GraphTraversalSource g) {
        return new SecondOrderQueryBuilder(g);
    }

    public Set<Vertex> execute() {
        RustJNI jni = new RustJNI();
        CommunityGraph graph = (CommunityGraph) g.getGraph();
        long graphHandle = graph.handle();
        long vertexHandle = ((CommunityVertex)vertex).handle();
        long[] bfsAnswer = jni.getBfsVertices(graphHandle, vertexHandle);
        int len = bfsAnswer.length;
        Set<Vertex> answer = new HashSet<>();
        for (int i = 0; i < len; i += 2) {
            answer.add(new CommunityVertex(graph, bfsAnswer[i]));
        }
        return answer;
    }

    public Map<String, Object> executeForWeb() {
        Set<Vertex> answer = execute();
        Set<Set<Vertex>> result = new HashSet<>();
        result.add(answer);
        return VsetResultSerializer.serialize(result);
    }
}
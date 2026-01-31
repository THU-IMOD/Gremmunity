package com.graph.rocks;

import com.graph.rocks.community.CommunityGraph;
import com.graph.rocks.so.SecondOrderTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import com.graph.rocks.serialize.VsetResultSerializer;

import java.util.*;

public class Test {

    public static void main(String[] args) {

        try (CommunityGraph graph = CommunityGraph.open("example"); graph) {
            graph.loadVertexProperty("exampleVertexProperty.json");
            graph.loadEdgeProperty("exampleEdgeProperty.csv");
            SecondOrderTraversalSource g = (SecondOrderTraversalSource) graph.traversal();
            boolean ans = g.SecondOrder()
                    .forall("x")
                    .exist("y")
                    .filter("g.V(x).bothE().otherV().is(y)")
                    .execute();

            Set<Set<Vertex>> sccs = g.SCC().execute();
            Set<Set<Object>> results = new HashSet<>();
            for (Set<Vertex> vertexSet: sccs) {
                Set<Object> nameSet = new HashSet<>();
                for (Vertex vertex: vertexSet) {
                    nameSet.add(vertex.value("city"));
                }
                results.add(nameSet);
            }
            System.out.println("Valid vertex subsets (by city): " + results);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            System.out.println("\n=== Graph Database Closed ===");
        }
    }
}
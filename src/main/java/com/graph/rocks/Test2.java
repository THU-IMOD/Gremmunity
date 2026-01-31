package com.graph.rocks;

import com.graph.rocks.community.CommunityGraph;
import com.graph.rocks.so.SecondOrderTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import com.graph.rocks.serialize.VsetResultSerializer;
import com.graph.rocks.community.CommunityVertex;

import java.util.*;

public class Test2 {

    public static void main(String[] args) {

        try (CommunityGraph graph = CommunityGraph.open("example2")) { // 原代码这里多了一个 ", graph"，属于语法冗余，已移除
            SecondOrderTraversalSource g = graph.traversal(SecondOrderTraversalSource.class);
            // 修正1：所有字符串用双引号包裹，补充city属性
            Vertex alice = g.addV("person").property(T.id, 1).property("name", "Alice").property("city", "New York").next();
            Vertex bob = g.addV("person").property(T.id, 2).property("name", "Bob").property("city", "London").next();
            Vertex charlie = g.addV("person").property(T.id, 3).property("name", "Charlie").property("city", "New York").next();
            Vertex david = g.addV("person").property(T.id, 4).property("name", "David").property("city", "Paris").next();
            // 修正2：edge标签用双引号包裹
            alice.addEdge("knows", bob);
            bob.addEdge("knows", charlie);
            charlie.addEdge("knows", alice);
            System.out.println(g.E().toList().size());
            Set<Set<Vertex>> ans = g.SCC().execute();
            Set<Set<Object>> results = new HashSet<>();
            for (Set<Vertex> vertexSet: ans) {
                Set<Object> nameSet = new HashSet<>();
                for (Vertex vertex: vertexSet) {
                    // 修正3：city属性用双引号包裹
                    nameSet.add(vertex.value("name"));
                }
                results.add(nameSet);
            }
            System.out.println("Valid vertex subsets (by name): " + results);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            System.out.println("\n=== Graph Database Closed ===");
        }
    }
}
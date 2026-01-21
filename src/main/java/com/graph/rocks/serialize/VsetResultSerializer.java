package com.graph.rocks.serialize;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import java.util.*;

/**
 * Serializer for Vset query results
 * Converts Set<Set<Vertex>> to JSON-friendly format for web visualization
 */
public class VsetResultSerializer {

    /**
     * Serialize Vset result to Map format
     * Returns a structure like:
     * {
     *   "type": "VsetResult",
     *   "subsets": [
     *     {"vertices": [1, 2, 3], "size": 3, "properties": {...}},
     *     {"vertices": [1, 2], "size": 2, "properties": {...}},
     *     {"vertices": [], "size": 0, "properties": {}}  // empty set
     *   ],
     *   "totalCount": 3
     * }
     */
    public static Map<String, Object> serialize(Set<Set<Vertex>> vsetResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "VsetResult");

        List<Map<String, Object>> subsets = new ArrayList<>();

        for (Set<Vertex> vertexSet : vsetResult) {
            Map<String, Object> subset = new LinkedHashMap<>();

            // Extract vertex IDs
            List<Object> vertexIds = new ArrayList<>();
            Map<Object, Map<String, Object>> vertexProperties = new LinkedHashMap<>();

            for (Vertex v : vertexSet) {
                Object id = v.id();
                vertexIds.add(id);

                // Extract vertex properties
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("id", id);
                props.put("label", v.label());

                // Add all vertex properties
                v.keys().forEach(key -> {
                    props.put(key, v.property(key).value());
                });

                vertexProperties.put(id, props);
            }

            subset.put("vertices", vertexIds);
            subset.put("size", vertexIds.size());
            subset.put("properties", vertexProperties);

            subsets.add(subset);
        }

        result.put("subsets", subsets);
        result.put("totalCount", subsets.size());

        return result;
    }

    /**
     * Check if a query is a Vset query
     */
    public static boolean isVsetQuery(String query) {
        return query != null &&
                (query.contains(".Vset()") ||
                        query.contains("g.Vset()") ||
                        query.trim().startsWith("Vset()"));
    }
}

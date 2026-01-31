package com.graph.rocks.so;

import com.graph.rocks.utils.KDimensionalArray;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import groovy.lang.GroovyShell;

import java.util.*;

/**
 * Core executor for second-order logic queries on graph data using Gremlin and Groovy evaluation.
 * Supports existential (∃) and universal (∀) quantifiers over vertex sets, with manual parsing
 * of logical expressions (||, &&, !) to ensure consistent boolean evaluation results.
 */
@SuppressWarnings("all")
public class GroovyGremlinQueryExecutor {

    /**
     * Executes a Gremlin query string within a Groovy execution context with bound variables.
     * Converts traversal/iterable results to concrete lists for consistent return types.
     *
     * @param groovyQuery Gremlin query string to execute
     * @param variables Map of variable names to their bound objects (e.g., GraphTraversalSource)
     * @return Query result - List for traversals/iterables, raw primitive for booleans, null on execution failure
     */
    public static Object executeGremlinQuery(String groovyQuery, Map<String, Object> variables) {
        GroovyShell shell = new GroovyShell();

        // Bind all variables to Groovy execution context
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            shell.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            Object result = shell.evaluate(groovyQuery);
            if (result == null) return null;

            // Convert GraphTraversal results to List for consistent handling
            if (result instanceof org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal) {
                return ((org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal) result).toList();
            }

            // Convert Iterable results to List
            if (result instanceof Iterable) {
                List<Object> resultList = new ArrayList<>();
                for (Object item : (Iterable<?>) result) {
                    resultList.add(item);
                }
                return resultList;
            }

            // Return boolean primitives directly (no wrapping)
            if (result instanceof Boolean) {
                return result;
            }

            // Wrap single non-boolean/non-collection values in List for consistency
            return Collections.singletonList(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Manually parses and evaluates logical expressions with operator precedence:
     * parentheses → NOT (!) → AND (&&) → OR (||). Implements short-circuit evaluation.
     *
     * @param expression Logical expression string (supports ||, &&, !, and parentheses)
     * @param variables Variable bindings for expression evaluation
     * @return Boolean result of the expression evaluation
     */
    private static boolean evaluateLogicalExpression(String expression, Map<String, Object> variables) {
        expression = expression.trim();

        // Resolve nested parentheses recursively
        while (true) {
            String nextExpression = evaluateParentheses(expression, variables);
            if (nextExpression.equals(expression)) {
                break;
            }
            expression = nextExpression;
        }

        // Evaluate OR operator (lowest precedence) with short-circuit
        List<String> orParts = splitByOperator(expression, "||");
        if (orParts.size() > 1) {
            for (String part : orParts) {
                if (evaluateLogicalExpression(part.trim(), variables)) {
                    return true;
                }
            }
            return false;
        }

        // Evaluate AND operator (medium precedence) with short-circuit
        List<String> andParts = splitByOperator(expression, "&&");
        if (andParts.size() > 1) {
            for (String part : andParts) {
                if (!evaluateLogicalExpression(part.trim(), variables)) {
                    return false;
                }
            }
            return true;
        }

        // Evaluate NOT operator (highest precedence)
        if (expression.startsWith("!")) {
            String innerExpr = expression.substring(1).trim();
            return !evaluateLogicalExpression(innerExpr, variables);
        }

        // Evaluate basic expression (no logical operators)
        return evaluateBasicExpression(expression, variables);
    }

    /**
     * Finds and evaluates the innermost parentheses in an expression, replacing
     * the parentheses block with its boolean evaluation result.
     *
     * @param expression Expression containing parentheses to resolve
     * @param variables Variable bindings for evaluation
     * @return Modified expression with innermost parentheses replaced by boolean result
     */
    private static String evaluateParentheses(String expression, Map<String, Object> variables) {
        // Locate innermost opening parenthesis
        int openIndex = -1;
        int closeIndex = -1;
        int len = expression.length();

        for (int i = len - 1; i >= 0 ; i--) {
            if (expression.charAt(i) == '(') {
                if (i == 0) {
                    openIndex = i;
                    break;
                } else {
                    char pre = expression.charAt(i - 1);
                    if (!Character.isLetterOrDigit(pre)) {
                        openIndex = i;
                        break;
                    }
                }
            }
        }

        // Find matching closing parenthesis for the innermost opening
        if (openIndex != -1) {
            int badBracket = 0;
            for (int i = openIndex + 1; i < len; i++) {
                if (expression.charAt(i) == '(') {
                    badBracket++;
                }
                if (expression.charAt(i) == ')') {
                    if (badBracket == 0) {
                        closeIndex = i;
                        break;
                    }
                    badBracket--;
                }
            }
        }

        // No parentheses found - return original expression
        if (openIndex == -1 || closeIndex == -1) {
            return expression;
        }

        // Extract and evaluate inner expression
        String innerExpr = expression.substring(openIndex + 1, closeIndex);
        boolean result = evaluateLogicalExpression(innerExpr, variables);

        // Replace parentheses block with evaluation result
        String before = expression.substring(0, openIndex);
        String after = expression.substring(closeIndex + 1);

        return before + result + after;
    }

    /**
     * Splits an expression by a specified logical operator, ignoring operators
     * that are inside parentheses.
     *
     * @param expression Target expression to split
     * @param operator Logical operator to split on ("||" or "&&")
     * @return List of expression parts separated by the operator (outside parentheses)
     */
    private static List<String> splitByOperator(String expression, String operator) {
        List<String> parts = new ArrayList<>();
        int parenthesesLevel = 0;
        int lastSplit = 0;
        int len = expression.length();
        int opLen = operator.length();

        for (int i = 0; i < len; i++) {
            char c = expression.charAt(i);

            // Track parentheses depth
            if (c == '(') {
                parenthesesLevel++;
            } else if (c == ')') {
                parenthesesLevel--;
            }
            // Check for operator only when outside parentheses
            else if (parenthesesLevel == 0) {
                if (i <= len - opLen) {
                    String sub = expression.substring(i, i + opLen);
                    if (sub.equals(operator)) {
                        parts.add(expression.substring(lastSplit, i));
                        lastSplit = i + opLen;
                        i += opLen - 1; // Skip remaining operator characters
                    }
                }
            }
        }

        // Add the final part of the expression
        if (lastSplit < len) {
            parts.add(expression.substring(lastSplit));
        }

        // Return original expression if no operator found
        if (parts.isEmpty()) {
            parts.add(expression);
        }

        return parts;
    }

    /**
     * Evaluates a basic expression (no logical operators/parentheses) by executing
     * it as a Gremlin query and converting the result to a boolean value.
     *
     * @param expression Basic expression string (single Gremlin query)
     * @param variables Variable bindings for query execution
     * @return Boolean result - empty collections = false, non-empty = true, null = false
     */
    private static boolean evaluateBasicExpression(String expression, Map<String, Object> variables) {
        expression = expression.trim();

        // Direct boolean value handling
        if ("true".equalsIgnoreCase(expression)) {
            return true;
        }
        if ("false".equalsIgnoreCase(expression)) {
            return false;
        }

        // Execute Gremlin query and convert result to boolean
        Object result = executeGremlinQuery(expression, variables);
        if (result == null) {
            return false;
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        // Treat non-empty collections as true, empty as false
        if (result instanceof List) {
            return !((List<?>) result).isEmpty();
        }

        // Treat all other non-null types as true
        return true;
    }

    /**
     * Recursively precomputes evaluation results for all combinations of vertices
     * bound to quantifier variables, storing results in a k-dimensional array.
     *
     * @param g GraphTraversalSource for query execution
     * @param vertices List of vertices to iterate over
     * @param variables Variable bindings (will be modified during recursion)
     * @param groovyQuery Gremlin query to evaluate
     * @param conditions List of quantifier conditions (varName → "exist"/"forall")
     * @param coordinates Current position in the k-dimensional result array
     * @param results k-dimensional array to store precomputed results
     * @param index Current quantifier variable index in recursion
     */
    private static void calcSecondOrder(
            GraphTraversalSource g,
            List<Vertex> vertices,
            Map<String, Object> variables,
            String groovyQuery,
            List<Map.Entry<String, String>> conditions,
            int[] coordinates,
            KDimensionalArray results,
            int index
    ) {
        // Base case: all variables bound - store evaluation result
        if (index >= conditions.size()) {
            results.set(coordinates, evaluateLogicalExpression(groovyQuery, variables));
            return;
        }

        // Recursively bind each vertex to current quantifier variable
        Map.Entry<String, String> condition = conditions.get(index);
        String varName = condition.getKey();
        int len = vertices.size();

        for (int i = 0; i < len; i++) {
            variables.put(varName, vertices.get(i));
            coordinates[index] = i;
            calcSecondOrder(g, vertices, variables, groovyQuery, conditions, coordinates, results, index + 1);
        }

        // Clean up variable binding to avoid side effects
        variables.remove(varName);
    }

    /**
     * Recursively evaluates second-order logic conditions using precomputed results,
     * implementing short-circuit evaluation for existential/universal quantifiers.
     *
     * @param results Precomputed k-dimensional array of evaluation results
     * @param vertices List of vertex indices to iterate over
     * @param quantifier Boolean array indicating quantifier type (true = exist, false = forall)
     * @param variables Current vertex indices bound to each quantifier variable
     * @param k Total number of quantifier variables
     * @param index Current quantifier variable index in recursion
     * @return Boolean result of the quantifier evaluation
     */
    private static boolean enumerateSecondOrder(
            KDimensionalArray results,
            List<Integer> vertices,
            boolean[] quantifier,
            int[] variables,
            int k,
            int index) {

        // Base case: all variables bound - return precomputed result
        if (index >= k) {
            return results.get(variables);
        }

        // Recursively evaluate each vertex for current quantifier
        for (Integer vertex : vertices) {
            variables[index] = vertex;
            boolean evaluationResult = enumerateSecondOrder(results, vertices, quantifier, variables, k, index + 1);

            // Short-circuit evaluation for quantifiers
            if (quantifier[index] && evaluationResult) return true;  // Existential quantifier (∃)
            if (!quantifier[index] && !evaluationResult) return false; // Universal quantifier (∀)
        }

        // Final result when loop completes (no short-circuit trigger)
        return !quantifier[index];
    }

    /**
     * Preprocesses and precomputes evaluation results for all vertex combinations
     * for the given second-order logic query, storing results in a k-dimensional array.
     *
     * @param g GraphTraversalSource for query execution
     * @param vertices List of vertices to evaluate
     * @param groovyQuery Gremlin query to evaluate
     * @param conditions List of quantifier conditions (varName → "exist"/"forall")
     * @param results k-dimensional array to store precomputed results
     */
    public static void Preconditioning(
            GraphTraversalSource g,
            List<Vertex> vertices,
            String groovyQuery,
            List<Map.Entry<String, String>> conditions,
            KDimensionalArray results
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("g", g); // Bind graph traversal source to evaluation context
        int[] coordinates = new int[conditions.size()];
        calcSecondOrder(g, vertices, variables, groovyQuery, conditions, coordinates, results, 0);
    }

    /**
     * Executes a second-order logic query with existential/universal quantifiers
     * over all vertices in the graph.
     *
     * @param g GraphTraversalSource for query execution
     * @param groovyQuery Gremlin query/condition to evaluate
     * @param conditions List of quantifier conditions (varName → "exist"/"forall")
     * @return True if the second-order condition is satisfied, false otherwise
     */
    public static boolean secondOrderQuery(
            GraphTraversalSource g,
            String groovyQuery,
            List<Map.Entry<String, String>> conditions
    ) {
        // Get total vertex count and quantifier count
        int n = g.V().count().next().intValue();
        int k = conditions.size();

        // Initialize k-dimensional array for precomputed results
        KDimensionalArray results = new KDimensionalArray(n, k);
        List<Vertex> vertices = g.V().toList();

        // Precompute evaluation results for all vertex combinations
        Preconditioning(g, vertices, groovyQuery, conditions, results);

        // Prepare vertex indices and quantifier type array
        List<Integer> vertexIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            vertexIds.add(i);
        }

        boolean[] quantifier = new boolean[k];
        for (int i = 0; i < k; i++) {
            quantifier[i] = conditions.get(i).getValue().equals("exist");
        }

        // Evaluate second-order logic conditions
        return enumerateSecondOrder(results, vertexIds, quantifier, new int[k], k, 0);
    }

    /**
     * Recursively generates all possible vertex subsets (power set) and evaluates
     * each subset against the precomputed second-order logic results.
     *
     * @param results Precomputed k-dimensional array of evaluation results
     * @param vertices Full list of vertices in the graph
     * @param selectedVertices Current subset being built (vertex indices)
     * @param groovyQuery Gremlin condition to evaluate
     * @param quantifier Boolean array indicating quantifier type (true = exist, false = forall)
     * @param subsets Result set to collect valid vertex subsets
     * @param n Total number of vertices in the graph
     * @param k Number of quantifier variables
     * @param index Current vertex index in recursion
     */
    private static void enumerateVset(
            KDimensionalArray results,
            boolean[] aggregationTable,
            List<Vertex> vertices,
            List<Integer> selectedVertices,
            String groovyQuery,
            String aggregationQuery,
            boolean[] quantifier,
            Set<Set<Vertex>> subsets,
            int n,
            int k,
            int index) {

        // Base case: full subset built - evaluate and collect if valid
        if (index >= n) {
            int size = selectedVertices.size();
            if (!aggregationTable[size]) {
                return;
            }
            if (enumerateSecondOrder(results, selectedVertices, quantifier, new int[k], k, 0)) {
                Set<Vertex> validSubset = new HashSet<>();
                for (int i : selectedVertices) {
                    validSubset.add(vertices.get(i));
                }
                subsets.add(validSubset);
            }
            return;
        }

        // Include current vertex in subset
        selectedVertices.add(index);
        enumerateVset(results, aggregationTable, vertices, selectedVertices, groovyQuery, aggregationQuery, quantifier, subsets, n, k, index + 1);

        // Exclude current vertex from subset
        selectedVertices.remove(selectedVertices.size() - 1);
        enumerateVset(results, aggregationTable, vertices, selectedVertices, groovyQuery, aggregationQuery, quantifier, subsets, n, k, index + 1);
    }

    /**
     * Finds all vertex subsets that satisfy the given second-order logical condition.
     * Generates the power set of all vertices and filters by the logical condition.
     *
     * @param g GraphTraversalSource for query execution
     * @param groovyQuery Gremlin condition to evaluate
     * @param conditions List of quantifier conditions (varName → "exist"/"forall")
     * @return Set of valid vertex subsets that satisfy the logical condition
     */
    public static Set<Set<Vertex>> VsetQuery(
            GraphTraversalSource g,
            String groovyQuery,
            String aggregationQuery,
            List<Map.Entry<String, String>> conditions
    ) {
        // Get total vertex count and quantifier count
        int n = g.V().count().next().intValue();
        int k = conditions.size();

        // Precompute evaluation results
        KDimensionalArray results = new KDimensionalArray(n, k);
        List<Vertex> vertices = g.V().toList();
        Preconditioning(g, vertices, groovyQuery, conditions, results);
        boolean[] aggregationTable = new boolean[n + 1];
        for (int i = 0; i <= n; i++) {
            aggregationTable[i] = true;
            GroovyShell shell = new GroovyShell();
            shell.setVariable("size", i);
            Object result = shell.evaluate(aggregationQuery);
            if (result instanceof Boolean) {
                if (!((Boolean) result))
                    aggregationTable[i] = false;
            } else {
                aggregationTable[i] = false;
            }
        }

        // Prepare quantifier type array
        boolean[] quantifierTypes = new boolean[k];
        for (int i = 0; i < k; i++) {
            quantifierTypes[i] = conditions.get(i).getValue().equals("exist");
        }

        // Enumerate all vertex subsets and filter valid ones
        Set<Set<Vertex>> validSubsets = new HashSet<>();
        List<Integer> selectedVertices = new ArrayList<>();
        enumerateVset(results, aggregationTable, vertices, selectedVertices, groovyQuery, aggregationQuery, quantifierTypes, validSubsets, n, k, 0);

        return validSubsets;
    }

    public static Set<Set<Vertex>> CommunityQuery(
            GraphTraversalSource g,
            String groovyQuery,
            String aggregationQuery,
            List<Map.Entry<String, String>> conditions,
            Set<Set<Vertex>> communities
    ) {
        int k = conditions.size();
        boolean[] quantifier = new boolean[k];
        for (int i = 0; i < k; i++) {
            quantifier[i] = conditions.get(i).getValue().equals("exist");
        }
        Set<Set<Vertex>> validSubsets = new HashSet<>();
        for (Set<Vertex> community: communities) {
            int n = community.size();
            GroovyShell shell = new GroovyShell();
            shell.setVariable("size", n);
            Object result = shell.evaluate(aggregationQuery);
            if (result instanceof Boolean) {
                if (!((Boolean) result))
                    continue;
            } else {
                continue;
            }
            KDimensionalArray results = new KDimensionalArray(n, k);
            List<Vertex> vertices = new ArrayList<>(community);
            Preconditioning(g, vertices, groovyQuery, conditions, results);
            List<Integer> vertexIds = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                vertexIds.add(i);
            }
            boolean answer = enumerateSecondOrder(results, vertexIds, quantifier, new int[k], k, 0);
            if (answer) {
                validSubsets.add(community);
            }
        }
        return validSubsets;
    }
}
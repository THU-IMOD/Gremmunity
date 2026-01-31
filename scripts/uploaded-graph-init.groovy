// Auto-generated initialization script for uploaded graph
// Generated at: 2026-01-28T17:14:11.575122

// Initialize graph
graph.reload('example')
g = graph.traversal(SecondOrderTraversalSource.class)

// Load vertex properties
graph.loadVertexProperty('misakaVertexProperty.json')

// Load edge properties
graph.loadEdgeProperty('misakaEdgeProperty.csv')

println "Uploaded graph 'example' initialized"

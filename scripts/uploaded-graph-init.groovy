// Auto-generated initialization script for uploaded graph
// Generated at: 2026-01-22T00:01:12.780738700

// Initialize graph
graph.reload('misaka')
g = graph.traversal(SecondOrderTraversalSource.class)

// Load vertex properties
graph.loadVertexProperty('misakaVertexProperty.csv')

// Load edge properties
graph.loadEdgeProperty('misakaEdgeProperty.json')

println "Uploaded graph 'misaka' initialized"

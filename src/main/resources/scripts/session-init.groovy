println("=" * 80)
println("CUSTOM SCRIPT STARTING - session-init.groovy")
println("=" * 80)

try {
    graph = CommunityGraph.open('new')
    println("✓ Successfully opened 'new' database")

    g = graph.traversal(SecondOrderTraversalSource.class)
    println("✓ Successfully created SecondOrderTraversalSource")

    println("Gremmunity initialized with 'new' database")
} catch (Exception e) {
    println("✗ ERROR in session-init.groovy:")
    e.printStackTrace()
}

println("=" * 80)
println("CUSTOM SCRIPT ENDED")
println("=" * 80)
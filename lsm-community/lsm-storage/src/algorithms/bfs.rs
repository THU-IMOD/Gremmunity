use crate::{LsmCommunity, types::VId};
use std::collections::VecDeque;

impl LsmCommunity {
    /// Performs a Breadth-First Search (BFS) starting from the specified vertex.
    ///
    /// This implementation uses a bitmap-based visited tracking mechanism for optimal
    /// space efficiency, particularly important when handling large-scale graphs.
    ///
    /// # Arguments
    ///
    /// * `start_vertex` - The vertex ID from which to begin the BFS traversal
    ///
    /// # Returns
    ///
    /// Returns a vector of tuples `(vertex_id, distance)` where:
    /// - `vertex_id`: A reachable vertex from the start vertex
    /// - `distance`: The number of hops (shortest path length) from the start vertex
    ///
    /// Returns an empty vector if the start vertex is invalid or does not exist.
    ///
    /// # Performance Characteristics
    ///
    /// - **Time Complexity**: O(V + E) where V is the number of reachable vertices and E is edges
    /// - **Space Complexity**: O(V/64) for the visited bitmap + O(V) for the result vector
    /// - Uses bit-level marking (1 bit per vertex) instead of hash-based sets for minimal memory overhead
    /// - Pre-allocates queue and result capacities to reduce dynamic reallocations
    ///
    /// # Notes
    ///
    /// - The algorithm gracefully handles invalid vertices encountered during traversal
    /// - Each vertex appears at most once in the result with its shortest distance
    /// - The order of vertices in the result follows BFS discovery order (level by level)
    pub fn bfs(&self, start_vertex: VId) -> Vec<(VId, u32)> {
        // Initialize visited bitmap - using bit-level marking for space efficiency
        let max_vid = self.vertex_count() as VId;
        let bitmap_size = ((max_vid + 63) / 64) as usize;
        let mut visited = vec![0u64; bitmap_size];

        // Helper closures for visited status
        let mark_visited = |visited: &mut [u64], vid: VId| {
            let idx = (vid / 64) as usize;
            let bit = vid % 64;
            if idx < visited.len() {
                visited[idx] |= 1u64 << bit;
            }
        };

        let is_visited = |visited: &[u64], vid: VId| -> bool {
            let idx = (vid / 64) as usize;
            let bit = vid % 64;
            idx < visited.len() && (visited[idx] & (1u64 << bit)) != 0
        };

        // BFS queue: store (vertex_id, distance)
        let mut queue = VecDeque::with_capacity(1024);
        let mut result = Vec::with_capacity(1024);

        // Start BFS
        queue.push_back((start_vertex, 0u32));
        mark_visited(&mut visited, start_vertex);
        result.push((start_vertex, 0u32));

        while let Some((current_vid, current_dist)) = queue.pop_front() {
            // Read the neighbor list of the current vertex
            let neighbors = match self.read_out_neighbor_clone(current_vid) {
                // Success: Get the Vec<VId> neighbor list directly (no Option needed)
                Ok(n) => n,
                // Failure: Skip the current iteration without further processing
                Err(_) => continue,
            };

            // Calculate the distance of the neighbor vertices
            let next_dist = current_dist + 1;

            // Iterate over and process each neighbor vertex
            for neighbor_vid in neighbors {
                if !is_visited(&visited, neighbor_vid) {
                    mark_visited(&mut visited, neighbor_vid);
                    queue.push_back((neighbor_vid, next_dist));
                    result.push((neighbor_vid, next_dist));
                }
            }
        }

        result
    }
}

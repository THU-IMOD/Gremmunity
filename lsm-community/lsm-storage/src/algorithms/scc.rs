use rustc_hash::FxHashSet; // Add `rustc-hash = "1.1.0"` to Cargo.toml
use crate::{LsmCommunity, types::VId, delta::DeltaOpType};

impl LsmCommunity {
    /// Compute Strongly Connected Components using non-recursive Tarjan's algorithm.
    ///
    /// In a directed graph, an SCC is a maximal set of vertices where every vertex
    /// is reachable from every other vertex in the set. This implementation uses
    /// a non-recursive version of Tarjan's algorithm to avoid stack overflow on
    /// large graphs.
    ///
    /// # Returns
    ///
    /// Returns a vector where `result[vertex_id]` contains the component ID
    /// that the vertex belongs to. Vertices with the same component ID are
    /// in the same strongly connected component.
    ///
    /// # Performance
    ///
    /// - Time: O(V + E)
    /// - Space: O(V + E)
    pub fn scc(&self) -> Vec<VId> {
        let vertex_count = self.vertex_count();
        let vertex_index_state = self.vertex_index.read();

        // Pre-load all neighbors (with delta log applied) to avoid repeated reads and clones
        let all_neighbors: Vec<Vec<VId>> = (0..vertex_count as VId)
            .map(|vid| {
                let mut base_neighbors = FxHashSet::default();

                if let Ok((neighbors_iter, delta_log)) =
                    self.read_neighbor_hold_index_vertex(vid, true, &vertex_index_state)
                {
                    // 1. Load base neighbor list into FxHashSet for O(1) incremental operations
                    if let Some(neighbors) = neighbors_iter {
                        base_neighbors = neighbors.collect::<FxHashSet<VId>>();
                    }

                    // 2. Apply delta log operations (Add/Remove) to update neighbor list
                    if let Some(delta) = delta_log {
                        for op in delta.ops {
                            match op.get_op_type() {
                                Some(DeltaOpType::AddNeighbor) => {
                                    // Add neighbor
                                    base_neighbors.insert(op.neighbor);
                                }
                                Some(DeltaOpType::RemoveNeighbor) => {
                                    // Remove neighbor
                                    base_neighbors.remove(&op.neighbor);
                                }
                                None => {
                                    // Ignore invalid or unrecognized operation types
                                    continue;
                                }
                            }
                        }
                    }
                }

                // Convert HashSet back to Vec for consistent traversal in Tarjan's algorithm
                base_neighbors.into_iter().collect()
            })
            .collect();

        // Initialize data structures for Tarjan's algorithm
        let mut dfn = vec![u32::MAX; vertex_count as usize];  // DFS discovery timestamp
        let mut low = vec![u32::MAX; vertex_count as usize];  // Low-link value (smallest discovery timestamp reachable)
        let mut on_stack = vec![false; vertex_count as usize]; // Flag indicating if vertex is in the SCC stack
        let mut scc_stack = Vec::new();                        // Stack to track current path of DFS
        let mut scc_id = vec![0 as VId; vertex_count as usize]; // Final component ID for each vertex

        let mut timestamp: u32 = 0;
        let mut current_scc_id: VId = 0;

        // Non-recursive DFS state to avoid stack overflow on large graphs
        enum State {
            FirstVisit,
            AfterChild(usize), // Index of the child vertex that was just processed
        }

        struct StackFrame {
            node: VId,
            state: State,
        }

        let mut dfs_stack: Vec<StackFrame> = Vec::new();

        // Process all unvisited vertices to cover disconnected graphs
        for start_node in 0..vertex_count as VId {
            if dfn[start_node as usize] != u32::MAX {
                continue; // Skip vertices that have already been visited
            }

            // Initialize DFS traversal from the current start node
            dfs_stack.push(StackFrame {
                node: start_node,
                state: State::FirstVisit,
            });

            while let Some(frame) = dfs_stack.pop() {
                let u = frame.node;
                let u_idx = u as usize;
                let neighbors = &all_neighbors[u_idx];

                match frame.state {
                    State::FirstVisit => {
                        // First time visiting this node - initialize discovery and low-link values
                        dfn[u_idx] = timestamp;
                        low[u_idx] = timestamp;
                        timestamp += 1;
                        scc_stack.push(u);
                        on_stack[u_idx] = true;

                        if neighbors.is_empty() {
                            // No outgoing edges - check if this node is the root of an SCC
                            if dfn[u_idx] == low[u_idx] {
                                // Pop the SCC from the stack and assign component ID
                                loop {
                                    let v = scc_stack.pop().unwrap();
                                    on_stack[v as usize] = false;
                                    scc_id[v as usize] = current_scc_id;
                                    if v == u {
                                        break;
                                    }
                                }
                                current_scc_id += 1;
                            }
                        } else {
                            // Has outgoing edges - push frame back to process after children
                            dfs_stack.push(StackFrame {
                                node: u,
                                state: State::AfterChild(0),
                            });

                            // Process the first child vertex
                            let v = neighbors[0];
                            let v_idx = v as usize;

                            if dfn[v_idx] == u32::MAX {
                                // Child vertex not visited - initiate DFS for the child
                                dfs_stack.push(StackFrame {
                                    node: v,
                                    state: State::FirstVisit,
                                });
                            } else if on_stack[v_idx] {
                                // Child vertex is in the current DFS path - update low-link value
                                low[u_idx] = low[u_idx].min(dfn[v_idx]);
                            }
                        }
                    }
                    State::AfterChild(child_idx) => {
                        // Returned from processing the child at the given index - update low-link value
                        if child_idx < neighbors.len() {
                            let v = neighbors[child_idx];
                            let v_idx = v as usize;
                            // Only update low-link for tree edges (child was discovered after current node)
                            if dfn[v_idx] > dfn[u_idx] {
                                low[u_idx] = low[u_idx].min(low[v_idx]);
                            }
                        }

                        // Check if there are remaining children to process
                        let next_child_idx = child_idx + 1;
                        if next_child_idx < neighbors.len() {
                            // Push frame back to process next child
                            dfs_stack.push(StackFrame {
                                node: u,
                                state: State::AfterChild(next_child_idx),
                            });

                            // Process the next child vertex
                            let v = neighbors[next_child_idx];
                            let v_idx = v as usize;

                            if dfn[v_idx] == u32::MAX {
                                // Child vertex not visited - initiate DFS for the child
                                dfs_stack.push(StackFrame {
                                    node: v,
                                    state: State::FirstVisit,
                                });
                            } else if on_stack[v_idx] {
                                // Child vertex is in the current DFS path - update low-link value
                                low[u_idx] = low[u_idx].min(dfn[v_idx]);
                            }
                        } else {
                            // All children processed - check if current node is the root of an SCC
                            if dfn[u_idx] == low[u_idx] {
                                // Pop the SCC from the stack and assign component ID
                                loop {
                                    let v = scc_stack.pop().unwrap();
                                    on_stack[v as usize] = false;
                                    scc_id[v as usize] = current_scc_id;
                                    if v == u {
                                        break;
                                    }
                                }
                                current_scc_id += 1;
                            }
                        }
                    }
                }
            }
        }

        // Return the final SCC component IDs for all vertices
        scc_id
    }
}
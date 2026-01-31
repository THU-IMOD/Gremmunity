// src/scc.rs
use anyhow::Result;
use clap::Parser;
use lsm_storage::{LsmCommunity, LsmCommunityStorageOptions};
use std::time::Instant;

/// SCC (Strongly Connected Components) benchmark for LSM graph storage
#[derive(Parser, Debug)]
#[command(name = "scc")]
#[command(about = "Compute strongly connected components and measure performance", long_about = None)]
struct Args {
    /// Graph name to load
    #[arg(short, long, default_value = "sd")]
    graph: String,
}

fn main() -> Result<()> {
    let args = Args::parse();

    // Setup storage options
    let mut options = LsmCommunityStorageOptions::default();
    options.graph_name = args.graph.clone();

    let lsm_community = LsmCommunity::open(options)?;
    lsm_community.warm_up()?;

    // Run SCC
    println!("Computing Strongly Connected Components...");
    let scc_start = Instant::now();
    let scc_result = lsm_community.scc();
    let scc_time = scc_start.elapsed();
    println!("Computing Strongly Connected Components - [OK]");

    // Compute statistics
    let mut component_sizes = std::collections::HashMap::new();
    for &comp_id in &scc_result {
        *component_sizes.entry(comp_id).or_insert(0) += 1;
    }

    let num_components = component_sizes.len();

    // Report results
    println!("\n=== SCC Results ===");
    println!("Execution time: {:.2} ms", scc_time.as_secs_f64() * 1000.0);
    println!("Total vertices: {}", scc_result.len());
    println!("Number of components: {}", num_components);

    Ok(())
}

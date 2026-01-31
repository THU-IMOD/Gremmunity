# Gremmunity Demo - Vue Frontend

> A modern web interface for Gremmunity second-order graph query system

## 🎯 Features

- ✅ **Real-time Graph Visualization** - Interactive graph visualization using Cytoscape.js
- ✅ **Query Editor** - Easy-to-use query editor with syntax highlighting
- ✅ **Multiple Views** - Table, JSON, and Statistics views for query results
- ✅ **Second-Order Logic** - Full support for ∀ (forall) and ∃ (exist) quantifiers
- ✅ **Example Queries** - Built-in examples for common use cases
- ✅ **Query History** - Track and reuse previous queries
- ✅ **Responsive UI** - Modern, responsive design using Element Plus

## 📋 Prerequisites

- Node.js >= 16.0.0
- npm or yarn
- Gremlin Server running on localhost:8182 (with Gremmunity extensions)

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd vue-demo
npm install
```

### 2. Start Gremlin Server

Make sure your Gremlin Server is running with the SecondOrderTraversalSource:

```bash
# In your Gremlin Server directory
java -jar gremlin-server.jar conf/gremlin-server.yaml
```

### 3. Start Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### 4. Connect to Gremlin Server

1. Open the application in your browser
2. Click "Connect" button
3. Enter server details (default: localhost:8182)
4. Click "Connect"

## 📁 Project Structure

```
vue-demo/
├── src/
│   ├── components/
│   │   ├── QueryEditor.vue          # Query input component
│   │   ├── GraphVisualization.vue   # Cytoscape graph visualization
│   │   └── ResultsPanel.vue         # Query results display
│   ├── services/
│   │   └── gremlinClient.js         # Gremlin Server client
│   ├── store/
│   │   └── graphStore.js            # Pinia state management
│   ├── App.vue                      # Main application component
│   ├── main.js                      # Application entry point
│   └── style.css                    # Global styles
├── package.json
├── vite.config.js
└── index.html
```

## 🎨 Screenshots

### Main Interface
[Graph visualization with query editor and results panel]

### Example Queries
Built-in examples for:
- Basic Gremlin queries
- Second-order logic queries
- Vertex set queries (Vset)

## 📝 Usage Examples

### Basic Query

```javascript
// Get all vertices
g.V().valueMap(true).toList()
```

### Second-Order Query

```javascript
// Check if everyone knows someone
g.secondOrder()
  .forall('x')
  .exist('y')
  .filter('g.V(x).out("knows").is(y)')
  .execute()
```

### Vertex Set Query

```javascript
// Find all cliques
g.Vset()
  .forall('x')
  .forall('y')
  .filter('g.V(x).out("knows").is(y) || g.V(x).is(y)')
  .execute()
```

## 🔧 Configuration

### Vite Proxy

The Vite dev server is configured to proxy requests to the Gremlin Server:

```javascript
// vite.config.js
server: {
  proxy: {
    '/gremlin': {
      target: 'http://localhost:8182',
      changeOrigin: true
    }
  }
}
```

### Gremlin Server Setup

Make sure your `gremlin-server.yaml` includes:

```yaml
graphs: {
  graph: conf/community-graph.properties
}

scriptEngines: {
  gremlin-groovy: {
    imports: [
      com.graph.rocks.so.SecondOrderTraversalSource
    ]
  }
}
```

## 🏗️ Build for Production

```bash
# Build
npm run build

# Preview production build
npm run preview
```

The built files will be in the `dist/` directory.

## 🐛 Troubleshooting

### Connection Failed

**Problem**: Cannot connect to Gremlin Server

**Solutions**:
1. Make sure Gremlin Server is running on port 8182
2. Check firewall settings
3. Verify server configuration in `gremlin-server.yaml`

### Graph Not Displaying

**Problem**: Graph visualization is empty

**Solutions**:
1. Create test data using "Create Test Data" button
2. Refresh graph using "Refresh Graph" button
3. Check browser console for errors

### Query Errors

**Problem**: Queries fail with errors

**Solutions**:
1. Ensure SecondOrderTraversalSource is initialized
2. Check query syntax
3. Verify graph data exists

## 🎯 For SIGIR Demo Paper

This interface is designed for the SIGIR 2026 Demo Paper submission. Key features for demo:

### Demo Scenarios

1. **Academic Collaborator Recommendation**
2. **Social Community Detection**
3. **Product Recommendation**
4. **Knowledge Graph Search**
5. **Citation Analysis**

### Recording Demo Video

1. Start with connection screen
2. Load example dataset
3. Run each demo scenario
4. Show graph visualization
5. Display query results
6. Show performance metrics

## 📚 Dependencies

- **Vue 3** - Progressive JavaScript framework
- **Vite** - Next generation frontend tooling
- **Element Plus** - Vue 3 component library
- **Pinia** - Vue Store (state management)
- **Cytoscape.js** - Graph visualization library
- **Gremlin JavaScript** - Apache TinkerPop Gremlin client

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

[Your License Here]

## 🔗 Links

- [Gremlin Documentation](https://tinkerpop.apache.org/docs/current/reference/)
- [Cytoscape.js](https://js.cytoscape.org/)
- [Element Plus](https://element-plus.org/)
- [Vue 3](https://vuejs.org/)

## 📧 Contact

For questions or support, please contact [your-email@example.com]

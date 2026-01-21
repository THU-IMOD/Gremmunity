# 🚀 Gremmunity Vue Demo - Complete Deployment Guide

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Backend Setup (Gremlin Server)](#backend-setup)
3. [Frontend Setup (Vue Application)](#frontend-setup)
4. [Testing](#testing)
5. [Troubleshooting](#troubleshooting)
6. [Production Deployment](#production-deployment)

---

## 1. Prerequisites

### Software Requirements

```bash
# Node.js (v16 or higher)
node --version  # Should be >= 16.0.0
npm --version   # Should be >= 8.0.0

# Java (for Gremlin Server)
java -version   # Should be >= 11

# Git
git --version
```

### File Structure

```
project-root/
├── gremlin-server/           # Your Gremlin Server
│   ├── conf/
│   │   └── gremlin-server.yaml
│   ├── scripts/
│   │   └── session-init.groovy
│   └── lib/
│       └── community-graph.jar
└── vue-demo/                 # Vue frontend (this folder)
    ├── src/
    ├── package.json
    └── README.md
```

---

## 2. Backend Setup (Gremlin Server)

### Step 1: Prepare Gremlin Server

1. **Ensure GremmnuityServer.java is running**

```bash
cd gremlin-server
java -jar gremlin-server.jar conf/gremlin-server.yaml
```

2. **Verify server is running**

```bash
# Should see output like:
# [INFO] GremlinServer - Gremlin Server configured with worker thread pool...
# [INFO] GremlinServer - Channel started at port 8182
```

### Step 2: Configure Session Initialization

Create or update `conf/session-init.groovy`:

```groovy
// session-init.groovy
import com.graph.rocks.community.CommunityGraph
import com.graph.rocks.so.SecondOrderTraversalSource

// Initialize graph
graph = CommunityGraph.open('demo_graph')

// Create second-order traversal source
g = graph.traversal(SecondOrderTraversalSource.class)

// Return confirmation
'SecondOrder session initialized'
```

### Step 3: Update gremlin-server.yaml

Ensure the following configuration:

```yaml
# gremlin-server.yaml
host: 0.0.0.0
port: 8182

graphs: {
  graph: conf/community-graph.properties
}

scriptEngines: {
  gremlin-groovy: {
    imports: [
      com.graph.rocks.so.SecondOrderTraversalSource,
      com.graph.rocks.community.CommunityGraph
    ],
    staticImports: [],
    scripts: [scripts/session-init.groovy]
  }
}

serializers:
  - { className: org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV3 }

# Enable WebSocket
channelizer: org.apache.tinkerpop.gremlin.server.channel.WebSocketChannelizer
```

---

## 3. Frontend Setup (Vue Application)

### Step 1: Install Dependencies

```bash
cd vue-demo
npm install
```

Expected dependencies:
- vue: ^3.4.0
- element-plus: ^2.4.4
- cytoscape: ^3.28.1
- gremlin: ^3.7.0
- pinia: ^2.1.7
- axios: ^1.6.2

### Step 2: Configure Connection

Check `vite.config.js` proxy settings:

```javascript
// vite.config.js
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/gremlin': {
        target: 'http://localhost:8182',
        changeOrigin: true
      }
    }
  }
})
```

### Step 3: Start Development Server

```bash
npm run dev
```

Expected output:
```
VITE v5.0.0  ready in 500 ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
➜  press h to show help
```

### Step 4: Access Application

Open browser and navigate to:
```
http://localhost:5173
```

---

## 4. Testing

### Test 1: Connection

1. Click "Connect" button
2. Enter:
   - Host: `localhost`
   - Port: `8182`
3. Click "Connect"
4. Should see "✓ Connected to Gremlin Server"

### Test 2: Create Test Data

1. Click "Create Test Data" button
2. Confirm the action
3. Should see:
   - 4 vertices (Alice, Bob, Charlie, David)
   - 3 edges (knows relationships)
   - Graph visualization updated

### Test 3: Run Basic Query

1. In Query Editor, enter:
```javascript
g.V().valueMap(true).toList()
```
2. Click "Execute" or press Ctrl+Enter
3. Should see:
   - Results panel appears
   - Table view with vertex data
   - JSON view available
   - Execution time displayed

### Test 4: Run Second-Order Query

1. Click "Examples" tab in sidebar
2. Select "Second-Order: Everyone Knows Someone"
3. Click "Use This Query"
4. Click "Execute"
5. Should see:
   - Result: `false` (because David is isolated)
   - Execution time
   - No errors

### Test 5: Graph Visualization

1. Click nodes in the graph
2. Should see:
   - Node details panel
   - Properties displayed
3. Try different layouts:
   - Click "Layout" button
   - Cycles through: cose-bilkent → circle → grid

---

## 5. Troubleshooting

### Problem 1: Cannot Connect to Server

**Symptoms**:
- "Connection failed" error
- Red "Disconnected" status

**Solutions**:

```bash
# 1. Check if server is running
netstat -an | grep 8182
# Should show: tcp  0.0.0.0:8182  LISTEN

# 2. Check server logs
tail -f logs/gremlin-server.log

# 3. Test with curl
curl http://localhost:8182
# Should get a response

# 4. Restart server
pkill -f gremlin-server
java -jar gremlin-server.jar conf/gremlin-server.yaml
```

### Problem 2: Graph Not Displaying

**Symptoms**:
- Empty graph visualization
- "0 nodes, 0 edges" displayed

**Solutions**:

```bash
# 1. Create test data
# Click "Create Test Data" button in UI

# 2. Check data exists
# In Gremlin Console:
:remote connect tinkerpop.server conf/remote.yaml
:remote console
g.V().count()  // Should return > 0

# 3. Refresh graph
# Click "Refresh Graph" button in UI
```

### Problem 3: Query Errors

**Symptoms**:
- "Query failed" error
- Error message displayed

**Common Errors and Fixes**:

```javascript
// Error: "No such property: graph"
// Fix: Ensure session-init.groovy creates graph
graph = CommunityGraph.open('demo_graph')

// Error: "secondOrder() is not a function"
// Fix: Ensure SecondOrderTraversalSource is initialized
g = graph.traversal(SecondOrderTraversalSource.class)

// Error: "ClassCastException: Boolean cannot be cast to List"
// Fix: Use the updated GroovyGremlinQueryExecutor.java with manual parsing
```

### Problem 4: NPM Install Failures

**Symptoms**:
- Dependency installation fails
- Missing modules errors

**Solutions**:

```bash
# 1. Clear npm cache
npm cache clean --force

# 2. Delete node_modules and lock file
rm -rf node_modules package-lock.json

# 3. Reinstall
npm install

# 4. If still fails, try specific versions
npm install cytoscape@3.28.1 --save
npm install gremlin@3.7.0 --save
```

### Problem 5: CORS Errors

**Symptoms**:
- "CORS policy" errors in browser console
- Requests blocked

**Solutions**:

Option 1: Use Vite proxy (already configured)
```javascript
// vite.config.js already has proxy
```

Option 2: Configure Gremlin Server CORS
```yaml
# gremlin-server.yaml
cors:
  enabled: true
  allowedOrigins: ["http://localhost:5173"]
```

---

## 6. Production Deployment

### Option 1: Static Hosting (Recommended for Demo)

```bash
# 1. Build production bundle
npm run build

# 2. Files will be in dist/
ls -la dist/

# 3. Serve with any static server
# Using Python
cd dist
python -m http.server 8080

# Using Node.js
npx serve dist -p 8080

# Using nginx
cp -r dist/* /var/www/html/gremmunity/
```

### Option 2: Docker Deployment

Create `Dockerfile`:

```dockerfile
# Build stage
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Create `nginx.conf`:

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /gremlin {
        proxy_pass http://gremlin-server:8182;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

Build and run:

```bash
# Build image
docker build -t gremmunity-demo .

# Run container
docker run -d -p 8080:80 --name gremmunity gremmunity-demo
```

### Option 3: Docker Compose (Full Stack)

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  gremlin-server:
    image: tinkerpop/gremlin-server:latest
    ports:
      - "8182:8182"
    volumes:
      - ./gremlin-server/conf:/opt/gremlin-server/conf
      - ./gremlin-server/scripts:/opt/gremlin-server/scripts
    
  gremmunity-demo:
    build: ./vue-demo
    ports:
      - "8080:80"
    depends_on:
      - gremlin-server
    environment:
      - GREMLIN_SERVER_URL=http://gremlin-server:8182
```

Run:

```bash
docker-compose up -d
```

---

## 📊 Performance Checklist

### Before Demo/Presentation

- [ ] Server is running and responding
- [ ] Test data is loaded
- [ ] All example queries work
- [ ] Graph visualization displays correctly
- [ ] Browser cache cleared
- [ ] Network is stable
- [ ] Backup data prepared

### During Demo

- [ ] Start with connection screen
- [ ] Show graph statistics
- [ ] Run 3-5 example queries
- [ ] Demonstrate graph interactions
- [ ] Show performance metrics
- [ ] Highlight second-order queries

---

## 🎥 Recording Demo Video

### Setup

```bash
# 1. Open fresh browser window (incognito mode)
# 2. Start screen recording (OBS/QuickTime)
# 3. Zoom to 125% for better visibility
# 4. Close unnecessary tabs/windows
```

### Script (5 minutes)

```
00:00-00:30  Introduction and Connection
00:30-01:30  Create test data and explore graph
01:30-02:30  Run basic Gremlin queries
02:30-04:00  Demonstrate second-order logic queries
04:00-04:30  Show performance and features
04:30-05:00  Summary and links
```

---

## 🆘 Emergency Fixes

### Quick Reset

```bash
# Backend
pkill -f gremlin-server
rm -rf data/*
java -jar gremlin-server.jar conf/gremlin-server.yaml

# Frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Backup Commands

```bash
# Export graph data
# In Gremlin Console:
graph.io(graphml()).writeGraph("backup.graphml")

# Import graph data
graph.io(graphml()).readGraph("backup.graphml")
```

---

## 📞 Support

If you encounter issues not covered here:
1. Check browser console (F12)
2. Check Gremlin Server logs
3. Verify network connectivity
4. Test with curl/Postman
5. Contact: [your-email@example.com]

---

**Good luck with your demo! 🚀**

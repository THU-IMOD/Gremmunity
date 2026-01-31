<template>
  <div id="app" class="gremmunity-app">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <h1 class="app-title">
          <span class="logo">🔷</span>
          MonacGraph
        </h1>
        <span class="subtitle">Second-Order Graph Query System</span>
      </div>
      
      <div class="header-right">
        <!-- 连接状态 -->
        <el-tag :type="store.isConnected ? 'success' : 'danger'" size="large">
          <el-icon><Connection /></el-icon>
          {{ store.isConnected ? 'Connected' : 'Disconnected' }}
        </el-tag>

        <!-- 服务器信息 -->
        <span class="server-info" v-if="store.isConnected">
          {{ store.serverInfo.host }}:{{ store.serverInfo.port }}
        </span>

        <!-- 连接/断开按钮 -->
        <el-button 
          v-if="!store.isConnected"
          type="primary" 
          @click="showConnectionDialog = true"
        >
          <el-icon><Link /></el-icon>
          Connect
        </el-button>
        <el-button 
          v-else
          @click="handleDisconnect"
        >
          <el-icon><Close /></el-icon>
          Disconnect
        </el-button>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-container class="app-container">
      <!-- 左侧边栏 -->
      <el-aside width="300px" class="sidebar">
        <el-tabs v-model="activeTab" class="sidebar-tabs">
          <!-- 数据标签页 -->
          <el-tab-pane label="Data" name="data">
            <div class="tab-content">
              <h3>Graph Statistics</h3>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="Graph Name">
                  <el-tag type="info">{{ store.graphName || 'my_database' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Nodes">
                  {{ store.graphStats.nodeCount }}
                </el-descriptions-item>
                <el-descriptions-item label="Edges">
                  {{ store.graphStats.edgeCount }}
                </el-descriptions-item>
                <el-descriptions-item label="Node Types">
                  <el-tag 
                    v-for="type in store.graphStats.nodeTypes" 
                    :key="type"
                    size="small"
                    style="margin: 2px;"
                  >
                    {{ type }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>

              <el-divider />

              <h3>Actions</h3>
              <div class="action-buttons">
                <el-button 
                  type="success"
                  @click="showUploadDialog = true"
                  :disabled="!store.isConnected"
                  style="width: 100%;"
                >
                  <el-icon><Upload /></el-icon>
                  Upload Graph
                </el-button>

                <el-button 
                  type="primary" 
                  @click="handleRefresh"
                  :loading="isRefreshing"
                  :disabled="!store.isConnected"
                  style="width: 100%;"
                >
                  <el-icon><Refresh /></el-icon>
                  Refresh Graph
                </el-button>

                <el-button 
                  @click="handleCreateTestData"
                  :disabled="!store.isConnected"
                  style="width: 100%;"
                >
                  <el-icon><Plus /></el-icon>
                  Create Test Data
                </el-button>

                <el-button 
                  type="danger" 
                  @click="handleClearGraph"
                  :disabled="!store.isConnected"
                  style="width: 100%;"
                >
                  <el-icon><Delete /></el-icon>
                  Clear Graph
                </el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- 查询历史标签页 -->
          <el-tab-pane label="History" name="history">
            <div class="tab-content">
              <h3>Query History</h3>
              <el-scrollbar height="600px">
                <div 
                  v-for="(item, index) in store.recentQueries" 
                  :key="index"
                  class="history-item"
                  @click="handleSelectHistoryQuery(item.query)"
                >
                  <div class="history-query">{{ item.query }}</div>
                  <div class="history-meta">
                    <el-tag :type="item.success ? 'success' : 'danger'" size="small">
                      {{ item.success ? '✓' : '✗' }}
                    </el-tag>
                    <span class="history-time">{{ item.executionTime }}ms</span>
                  </div>
                </div>
              </el-scrollbar>
            </div>
          </el-tab-pane>

          <!-- 示例查询标签页 -->
          <el-tab-pane label="Examples" name="examples">
            <div class="tab-content">
              <h3>Example Queries</h3>
              <el-collapse>
                <el-collapse-item 
                  v-for="(example, index) in exampleQueries" 
                  :key="index"
                  :title="example.title"
                >
                  <div class="example-description">{{ example.description }}</div>
                  <el-button 
                    size="small" 
                    type="primary"
                    @click="handleSelectQuery(example.query)"
                  >
                    Use This Query
                  </el-button>
                  <pre class="example-code">{{ example.query }}</pre>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="main-content">
        <!-- 查询编辑器 -->
        <div class="query-section">
          <QueryEditor @execute="handleExecuteQuery" />
        </div>

        <!-- 分割线 -->
        <el-divider />

        <!-- 图可视化和结果面板 -->
        <el-container class="visualization-section">
          <!-- 图可视化或Vset浏览器 -->
          <el-main class="graph-container">
            <!-- Vset结果浏览器 -->
            <VsetBrowser
              v-if="store.queryResult && store.queryResult.isVset"
              :vset-result="store.queryResult.vsetResult"
            />
            <!-- 普通图可视化 -->
            <GraphVisualization v-else />
          </el-main>

          <!-- 结果面板（Vset查询时隐藏） -->
          <el-aside 
            width="400px" 
            class="results-panel" 
            v-if="store.queryResult && !store.queryResult.isVset"
          >
            <ResultsPanel />
          </el-aside>
        </el-container>
      </el-container>
    </el-container>

    <!-- 连接对话框 -->
    <el-dialog
      v-model="showConnectionDialog"
      title="Connect to MonacGraph Server"
      width="500px"
    >
      <el-form :model="connectionForm" label-width="100px">
        <el-form-item label="Host">
          <el-input v-model="connectionForm.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="Port">
          <el-input-number v-model="connectionForm.port" :min="1" :max="65535" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showConnectionDialog = false">Cancel</el-button>
        <el-button type="primary" @click="handleConnect" :loading="isConnecting">
          Connect
        </el-button>
      </template>
    </el-dialog>

    <!-- Graph Uploader Dialog -->
    <GraphUploader
      v-model:visible="showUploadDialog"
      @success="handleUploadSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useGraphStore } from '@/store/graphStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Link, Close, Refresh, Plus, Delete, Upload } from '@element-plus/icons-vue'
import QueryEditor from '@/components/QueryEditor.vue'
import GraphVisualization from '@/components/GraphVisualization.vue'
import ResultsPanel from '@/components/ResultsPanel.vue'
import VsetBrowser from '@/components/VsetBrowser.vue'
import GraphUploader from '@/components/GraphUploader.vue'

const store = useGraphStore()

// 状态
const activeTab = ref('data')
const showConnectionDialog = ref(false)
const showUploadDialog = ref(false)
const isConnecting = ref(false)
const isRefreshing = ref(false)

// 连接表单
const connectionForm = ref({
  host: 'localhost',
  port: 8182
})

// 示例查询
const exampleQueries = ref([
  {
    title: 'Create Test Data with Second-Order Query',
    description: 'Create 4 people and check connectivity',
    query: `g = graph.traversal(SecondOrderTraversalSource.class);
alice = g.addV('person').property(T.id, 1).property('name', 'Alice').next();
bob = g.addV('person').property(T.id, 2).property('name', 'Bob').next();
charlie = g.addV('person').property(T.id, 3).property('name', 'Charlie').next();
david = g.addV('person').property(T.id, 4).property('name', 'David').next();
alice.addEdge('knows', bob);
bob.addEdge('knows', charlie);
charlie.addEdge('knows', alice);
result = g.Vset().forall('x').forall('y').filter('g.V(x).out("knows").is(y) || g.V(y).out("knows").is(x) || g.V(x).is(y)').execute(); 
result.size()`
  },
  {
    title: 'Get All Vertices',
    description: 'Retrieve all vertices in the graph',
    query: 'g.V().elementMap().toList()'
  },
  {
    title: 'Count Vertices and Edges',
    description: 'Get basic graph statistics',
    query: `['vertices': g.V().count().next(), 'edges': g.E().count().next()]`
  },
  {
    title: 'Second-Order: Everyone Knows Someone',
    description: 'Check if every person knows at least one other person',
    query: `g.SecondOrder()
  .forall('x')
  .exist('y')
  .filter('g.V(x).out("knows").is(y)')
  .execute()`
  },
  {
    title: 'Vset: Find All Cliques with Size > 1',
    description: 'Find all cliques with size > 1 using second-order logic',
    query: `g.Vset()
  .forall('x')
  .forall('y')
  .filter('g.V(x).bothE().otherV().is(y) || g.V(x).is(y)')
  .having('size > 1')
  .executeForWeb()`
  },
  {
    title: 'WCC: Find Weakly Connected Components',
    description: 'Find all weakly connected components in the graph',
    query: 'g.WCC().executeForWeb()'
  },
  {
    title: 'SCC: Find Strongly Connected Components',
    description: 'Find all strongly connected components in the graph',
    query: 'g.SCC().executeForWeb()'
  },
  {
    title: 'Communities: Get LSM-Communities',
    description: 'Get all LSM-Communities in the graph',
    query: 'g.Community().executeForWeb()'
  },
  {
    title: 'BFS: Reachable Vertices',
    description: 'Find all vertices reachable from vertex 1',
    query: 'g.BFS(1).executeForWeb()'
  }
])

// 连接处理
const handleConnect = async () => {
  isConnecting.value = true
  try {
    const result = await store.connect(
      connectionForm.value.host,
      connectionForm.value.port
    )
    
    if (result.success) {
      ElMessage.success('Connected to Gremlin Server')
      showConnectionDialog.value = false
      // 加载图数据
      await store.refreshGraphData()
    } else {
      ElMessage.error(`Connection failed: ${result.message}`)
    }
  } finally {
    isConnecting.value = false
  }
}

const handleDisconnect = async () => {
  await store.disconnect()
  ElMessage.info('Disconnected from server')
}

// 查询处理
const handleExecuteQuery = async (query) => {
  const result = await store.executeQuery(query)
  
  if (result.success) {
    ElMessage.success(`Query executed in ${result.executionTime}ms`)
  } else {
    ElMessage.error(`Query failed: ${result.error}`)
  }
}

const handleSelectQuery = (query) => {
  store.currentQuery = query
}

const handleSelectHistoryQuery = (query) => {
  store.currentQuery = query
  activeTab.value = 'data'
}

// 数据操作
const handleRefresh = async () => {
  isRefreshing.value = true
  try {
    // 如果当前是Vset模式，清除查询结果
    if (store.queryResult && store.queryResult.isVset) {
      store.queryResult = null
      console.log('🔄 Cleared Vset result, switching to graph mode')
    }
    
    await store.refreshGraphData()
    ElMessage.success('Graph data refreshed')
  } finally {
    isRefreshing.value = false
  }
}

const handleCreateTestData = async () => {
  try {
    await ElMessageBox.confirm(
      'This will create test vertices (Alice, Bob, Charlie, David) and edges. Continue?',
      'Create Test Data',
      { type: 'info' }
    )
    
    await store.createTestData()
    ElMessage.success('Test data created')
  } catch {
    // User cancelled
  }
}

const handleClearGraph = async () => {
  try {
    await ElMessageBox.confirm(
      'This will delete all vertices and edges. This action cannot be undone!',
      'Clear Graph',
      { type: 'warning' }
    )
    
    await store.clearGraph()
    ElMessage.success('Graph cleared')
  } catch {
    // User cancelled
  }
}

const handleUploadSuccess = async (result) => {
  console.log('Graph uploaded successfully:', result)
  
  // Update the current graph name
  if (result.graphName) {
    store.graphName = result.graphName
    console.log('Updated graph name to:', result.graphName)
  }
  
  // Refresh graph data to show the newly loaded graph
  await handleRefresh()
  
  // Optionally, you can set the generated code to the query editor
  // (This would require adding a method to the store to update the current query)
}

// 初始化
onMounted(async () => {
  // 自动连接
  showConnectionDialog.value = true
})
</script>

<style scoped lang="scss">
.gremmunity-app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  .header-left {
    display: flex;
    align-items: center;
    gap: 15px;
  }

  .app-title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 8px;

    .logo {
      font-size: 28px;
    }
  }

  .subtitle {
    font-size: 14px;
    opacity: 0.9;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 15px;
  }

  .server-info {
    font-size: 13px;
    opacity: 0.9;
  }
}

.app-container {
  flex: 1;
  overflow: hidden;
}

.sidebar {
  background: white;
  border-right: 1px solid #e4e7ed;
  overflow: hidden;

  .sidebar-tabs {
    height: 100%;
    
    /* 标签等间距分布 */
    :deep(.el-tabs__header) {
      padding-left: 15px;
      padding-right: 15px;
      margin-bottom: 0;
    }
    
    :deep(.el-tabs__nav-wrap) {
      padding-left: 0;
    }
    
    :deep(.el-tabs__nav) {
      display: flex;
      width: 100%;
    }
    
    :deep(.el-tabs__item) {
      flex: 1;  /* 等分空间 */
      text-align: center;  /* 文字居中 */
      padding: 0 10px;
    }
    
    :deep(.el-tabs__content) {
      padding: 0;
    }
  }

  .tab-content {
    padding: 15px;

    h3 {
      margin: 0 0 10px 0;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
  }

  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 5px;
    
    /* 确保按钮完全对齐 */
    .el-button {
      width: 100%;
      margin: 0;  /* 移除默认margin */
      padding-left: 12px;
      padding-right: 12px;
      justify-content: flex-start;  /* 文字左对齐 */
      
      .el-icon {
        margin-right: 8px;
      }
    }
  }

  .history-item {
    padding: 10px;
    margin-bottom: 8px;
    background: #f5f7fa;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: #e4e7ed;
    }

    .history-query {
      font-size: 12px;
      font-family: 'Courier New', monospace;
      margin-bottom: 5px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .history-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      color: #909399;
    }
  }

  .example-description {
    margin-bottom: 10px;
    font-size: 13px;
    color: #606266;
  }

  .example-code {
    margin-top: 10px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 12px;
    font-family: 'Courier New', monospace;
    overflow-x: auto;
  }
}

.main-content {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.query-section {
  padding: 15px;
  background: white;
}

.visualization-section {
  flex: 1;
  overflow: hidden;
}

.graph-container {
  background: white;
  padding: 0;
  overflow: hidden;
}

.results-panel {
  background: white;
  border-left: 1px solid #e4e7ed;
  overflow: auto;
}
</style>

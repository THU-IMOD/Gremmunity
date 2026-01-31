<template>
  <div class="vset-browser">
    <!-- Header -->
    <div class="vset-header">
      <div class="vset-title">
        <el-icon><Collection /></el-icon>
        <h3>Vset Query Result</h3>
      </div>
      <div class="vset-info">
        <el-tag type="success">{{ totalSubsets }} subset(s) found</el-tag>
      </div>
    </div>

    <!-- Empty Result -->
    <div v-if="totalSubsets === 0" class="empty-result">
      <div class="empty-icon">Ø</div>
      <p>No subsets satisfy the condition</p>
    </div>

    <!-- Main Content -->
    <div v-else class="vset-main">
      <!-- Left: Graph and Navigation -->
      <div class="vset-left">
        <!-- Navigation -->
        <div class="subset-nav">
          <el-button
            :disabled="currentIndex === 0"
            @click="previousSubset"
            circle
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          
          <div class="subset-indicator">
            <span class="current">{{ currentIndex + 1 }}</span>
            <span class="separator">/</span>
            <span class="total">{{ totalSubsets }}</span>
          </div>
          
          <el-button
            :disabled="currentIndex === totalSubsets - 1"
            @click="nextSubset"
            circle
          >
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <!-- Current Subset Info -->
        <div class="current-subset-info">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="Index">
              {{ currentIndex + 1 }}
            </el-descriptions-item>
            <el-descriptions-item label="Size">
              {{ currentSubset.size }}
            </el-descriptions-item>
            <el-descriptions-item label="Vertices">
              {{ currentSubset.size === 0 ? 'Ø' : currentSubset.vertices.join(', ') }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Graph Visualization -->
        <div class="subset-visualization">
          <!-- Empty Set Display -->
          <div v-if="currentSubset.size === 0" class="empty-subset">
            <div class="empty-symbol">Ø</div>
            <p>Empty Set</p>
          </div>

          <!-- Graph -->
          <div v-else class="subset-graph" ref="subsetGraphContainer"></div>
        </div>

        <!-- Quick Jump -->
        <div class="quick-jump">
          <el-select
            v-model="currentIndex"
            placeholder="Jump to subset"
            size="small"
            style="width: 100%"
          >
            <el-option
              v-for="(subset, index) in subsets"
              :key="index"
              :label="`Subset ${index + 1} (${subset.size} vertices)`"
              :value="index"
            />
          </el-select>
        </div>
      </div>

      <!-- Right: Vertex List -->
      <div class="vset-right">
        <div class="vertex-list-panel">
          <h4>Vertices in Current Subset</h4>
          
          <!-- Empty Set -->
          <div v-if="currentSubset.size === 0" class="no-vertices">
            <el-empty description="Empty Set (Ø)" :image-size="80" />
          </div>
          
          <!-- Vertex Cards -->
          <el-scrollbar v-else class="vertex-scrollbar">
            <el-card
              v-for="(props, vertexId) in currentSubset.properties"
              :key="vertexId"
              class="vertex-card"
              shadow="hover"
            >
              <div class="vertex-info">
                <div class="vertex-id">
                  <el-tag type="primary" size="small">ID: {{ vertexId }}</el-tag>
                  <el-tag size="small">{{ props.label }}</el-tag>
                </div>
                <div class="vertex-props">
                  <span
                    v-for="(value, key) in filterProperties(props)"
                    :key="key"
                    class="prop-item"
                  >
                    <strong>{{ key }}:</strong> {{ value }}
                  </span>
                </div>
              </div>
            </el-card>
          </el-scrollbar>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Collection, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import cytoscape from 'cytoscape'

const props = defineProps({
  vsetResult: {
    type: Object,
    required: true
  }
})

// Vset data
const subsets = ref(props.vsetResult.subsets || [])
const totalSubsets = computed(() => subsets.value.length)
const currentIndex = ref(0)
const currentSubset = computed(() => subsets.value[currentIndex.value] || { size: 0, vertices: [], properties: {} })

// Cytoscape instance
const subsetGraphContainer = ref(null)
let cy = null
let isInitializing = false  // 防止重复初始化

// Navigation methods
const previousSubset = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextSubset = () => {
  if (currentIndex.value < totalSubsets.value - 1) {
    currentIndex.value++
  }
}

// Filter properties (remove id, label)
const filterProperties = (props) => {
  const filtered = { ...props }
  delete filtered.id
  delete filtered.label
  return filtered
}

// Initialize Cytoscape for current subset
const initSubsetGraph = async () => {
  // 防止重复初始化
  if (isInitializing) {
    console.log('⚠️ Already initializing, skipping...')
    return
  }
  
  // 检查子集是否为空
  if (currentSubset.value.size === 0) {
    console.log('⚠️ Empty subset, skipping graph init')
    return
  }
  
  console.log(`🔄 Starting graph init for subset ${currentIndex.value + 1} with ${currentSubset.value.size} nodes`)
  isInitializing = true

  try {
    // Wait for DOM update (multiple times for ref binding)
    await nextTick()
    await nextTick()
    await nextTick()
    
    // Wait for ref to be bound
    let refRetries = 0
    const maxRefRetries = 10
    
    while (!subsetGraphContainer.value && refRetries < maxRefRetries) {
      console.warn(`⏳ Waiting for container ref, retry ${refRetries + 1}/${maxRefRetries}`)
      await new Promise(resolve => setTimeout(resolve, 50))
      refRetries++
    }
    
    // Check if ref is available
    if (!subsetGraphContainer.value) {
      console.error('❌ No container ref after retries')
      isInitializing = false
      return
    }
    
    console.log('✅ Container ref found')
    
    // Wait for container to have size
    let sizeRetries = 0
    const maxSizeRetries = 10
    
    while (sizeRetries < maxSizeRetries) {
      const width = subsetGraphContainer.value.offsetWidth
      const height = subsetGraphContainer.value.offsetHeight
      
      if (width > 0 && height > 0) {
        console.log(`✅ Container ready: ${width}x${height}`)
        break
      }
      
      console.warn(`⏳ Container not ready (${width}x${height}), retry ${sizeRetries + 1}/${maxSizeRetries}`)
      await new Promise(resolve => setTimeout(resolve, 50))
      sizeRetries++
    }
    
    // Final check
    if (!subsetGraphContainer.value.offsetWidth || !subsetGraphContainer.value.offsetHeight) {
      console.error('❌ Container still has no size after retries')
      isInitializing = false
      return
    }

    // Destroy existing instance
    if (cy) {
      console.log('🗑️ Destroying old Cytoscape instance')
      cy.destroy()
      cy = null
    }

    // Create nodes from current subset
    const nodes = currentSubset.value.vertices.map(vertexId => {
      const props = currentSubset.value.properties[vertexId]
      return {
        data: {
          id: String(vertexId),
          label: props.label || 'node',
          ...props
        }
      }
    })

    console.log(`📊 Creating Cytoscape with ${nodes.length} nodes:`, nodes.map(n => n.data.id))

    // Create Cytoscape instance
    cy = cytoscape({
      container: subsetGraphContainer.value,
      elements: nodes,
      style: [
        {
          selector: 'node',
          style: {
            'label': function(ele) {
              const data = ele.data()
              if (data.name) {
                return data.name
              }
              const keys = Object.keys(data)
              const systemProps = ['id', 'label', '@type', '@value']
              const userProps = keys.filter(k => !systemProps.includes(k))
              
              if (userProps.length > 0) {
                return data[userProps[0]] || data.id || data.label || ''
              }
              return data.id || data.label || ''
            },
            'text-valign': 'center',
            'text-halign': 'center',
            'background-color': '#667eea',
            'color': '#fff',
            'font-size': '14px',
            'font-weight': 'bold',
            'width': '80px',
            'height': '80px',
            'border-width': 3,
            'border-color': '#ffffff',
            'text-outline-width': 2,
            'text-outline-color': '#667eea'
          }
        }
      ],
      layout: {
        name: 'circle',
        fit: true,
        padding: 50
      },
      minZoom: 0.5,
      maxZoom: 2,
      wheelSensitivity: 0.3
    })
    
    console.log(`✅ Cytoscape created, nodes in graph: ${cy.nodes().length}`)
    
    // Multiple attempts to center
    const centerGraph = () => {
      if (cy && cy.nodes().length > 0) {
        cy.fit(undefined, 50)
        cy.center()
        const zoom = cy.zoom()
        const pan = cy.pan()
        console.log(`📍 Graph centered - zoom: ${zoom.toFixed(2)}, pan: (${pan.x.toFixed(0)}, ${pan.y.toFixed(0)})`)
      }
    }
    
    // Immediate center
    centerGraph()
    
    // Delayed center (insurance)
    setTimeout(centerGraph, 50)
    setTimeout(centerGraph, 150)
    setTimeout(centerGraph, 300)
    
    console.log('✅ Graph initialization complete')
  } catch (error) {
    console.error('❌ Error initializing graph:', error)
  } finally {
    isInitializing = false
  }
}

// Watch for subset changes
watch(currentIndex, (newIndex, oldIndex) => {
  console.log(`🔀 Subset changed: ${oldIndex} → ${newIndex}`)
  initSubsetGraph()
})

// 🔥 Watch for vsetResult prop changes (fix for consecutive queries)
watch(() => props.vsetResult, (newResult) => {
  console.log('🔄 VsetResult prop changed, updating subsets')
  subsets.value = newResult.subsets || []
  currentIndex.value = 0  // Reset to first subset
  
  // Destroy old graph if it exists
  if (cy) {
    cy.destroy()
    cy = null
  }
  
  // Re-initialize graph with new data
  if (subsets.value.length > 0 && subsets.value[0].size > 0) {
    nextTick(() => {
      initSubsetGraph()
    })
  }
}, { deep: true })

// Initialize on mount
onMounted(() => {
  console.log(`🚀 VsetBrowser mounted with ${totalSubsets.value} subsets`)
  if (totalSubsets.value > 0 && currentSubset.value.size > 0) {
    initSubsetGraph()
  }
})

// Cleanup on unmount
onUnmounted(() => {
  if (cy) {
    cy.destroy()
  }
})
</script>

<style scoped lang="scss">
.vset-browser {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-height: calc(100vh - 180px);  // 最大高度，不是固定高度
  overflow-y: auto;  // 允许滚动
}

.vset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
  flex-shrink: 0;

  .vset-title {
    display: flex;
    align-items: center;
    gap: 10px;

    h3 {
      margin: 0;
      font-size: 16px;
      color: #303133;
    }
  }
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;

  .empty-icon {
    font-size: 80px;
    font-weight: bold;
    color: #dcdfe6;
    margin-bottom: 20px;
  }

  p {
    font-size: 16px;
    margin: 0;
  }
}

// Main two-column layout
.vset-main {
  display: flex;
  gap: 15px;
  // 移除 flex: 1 和 overflow: hidden，让其根据内容自动调整
}

// Left column: Graph and navigation
.vset-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

// Right column: Vertex list
.vset-right {
  width: 350px;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e4e7ed;
  padding-left: 15px;
  flex-shrink: 0;
}

.subset-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 6px;
  flex-shrink: 0;

  .subset-indicator {
    font-size: 16px;
    font-weight: bold;
    color: #303133;

    .current {
      color: #409eff;
    }

    .separator {
      margin: 0 8px;
      color: #909399;
    }

    .total {
      color: #606266;
    }
  }
}

.current-subset-info {
  flex-shrink: 0;
}

.subset-visualization {
  height: 320px;  // 固定高度，从 350px 调整到 330px
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  position: relative;
  flex-shrink: 0;  // 不压缩

  .empty-subset {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;

    .empty-symbol {
      font-size: 80px;  // 空集符号也缩小一点
      font-weight: bold;
      color: #dcdfe6;
      margin-bottom: 15px;
    }

    p {
      font-size: 16px;
      margin: 0;
    }
  }

  .subset-graph {
    width: 100%;
    height: 100%;
  }
}

.quick-jump {
  flex-shrink: 0;
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
}

// Vertex list panel (right side)
.vertex-list-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  
  h4 {
    margin: 0 0 12px 0;
    font-size: 14px;
    color: #303133;
    font-weight: 600;
    flex-shrink: 0;
  }

  .no-vertices {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .vertex-scrollbar {
    flex: 1;
    min-height: 0;
  }

  .vertex-card {
    margin-bottom: 10px;

    .vertex-info {
      .vertex-id {
        display: flex;
        gap: 8px;
        margin-bottom: 8px;
      }

      .vertex-props {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        font-size: 13px;
        color: #606266;

        .prop-item {
          strong {
            color: #303133;
          }
        }
      }
    }
  }
}
</style>

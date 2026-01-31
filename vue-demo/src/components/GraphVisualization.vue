<template>
  <div class="graph-visualization">
    <div class="graph-toolbar">
      <div class="toolbar-left">
        <h3>
          <el-icon><Histogram /></el-icon>
          Graph Visualization
        </h3>
        <el-tag type="info" size="small">
          {{ store.graphStats.nodeCount }} nodes, {{ store.graphStats.edgeCount }} edges
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button size="small" @click="fitGraph">
            <el-icon><FullScreen /></el-icon>
            Fit
          </el-button>
          <el-button size="small" @click="resetZoom">
            <el-icon><Refresh /></el-icon>
            Reset
          </el-button>
          <el-button size="small" @click="changeLayout">
            <el-icon><Grid /></el-icon>
            Layout: {{ currentLayout }}
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div ref="cyContainer" class="cy-container"></div>

    <!-- 节点详情面板 -->
    <el-card 
      v-if="selectedNode" 
      class="node-details"
      shadow="hover"
    >
      <template #header>
        <div class="card-header">
          <span>Node Details</span>
          <el-button 
            type="text" 
            @click="clearSelection"
            size="small"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item 
          v-for="(value, key) in selectedNode" 
          :key="key"
          :label="key"
        >
          {{ value }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useGraphStore } from '@/store/graphStore'
import cytoscape from 'cytoscape'
import coseBilkent from 'cytoscape-cose-bilkent'
import { Histogram, FullScreen, Refresh, Grid, Close } from '@element-plus/icons-vue'

// 注册布局
cytoscape.use(coseBilkent)

const store = useGraphStore()

const cyContainer = ref(null)
let cy = null

const currentLayout = ref('cose-bilkent')  // 默认使用cose-bilkent布局
const selectedNode = ref(null)

// 布局配置
const layouts = {
  'cose-bilkent': {
    name: 'cose-bilkent',
    quality: 'default',
    nodeDimensionsIncludeLabels: true,
    refresh: 30,
    fit: true,
    padding: 60,  // 减小padding，让图更充实
    randomize: true,
    // 减小理想边长，让节点更靠近
    idealEdgeLength: 120,
    edgeElasticity: 0.45,
    nestingFactor: 0.1,
    gravity: 0.6, // 增加引力，让节点更聚集
    numIter: 3000,
    tile: true,
    tilingPaddingVertical: 15,
    tilingPaddingHorizontal: 15,
    gravityRangeCompound: 1.5,
    gravityCompound: 1.0,
    gravityRange: 3.8,
    // 增加节点排斥力，避免重叠
    nodeRepulsion: 10000
  },
  'circle': {
    name: 'circle',
    fit: true,
    padding: 30,
    avoidOverlap: true,
    radius: undefined,
    startAngle: 3 / 2 * Math.PI,
    sweep: undefined,
    clockwise: true,
    sort: undefined,
    animate: false,
    animationDuration: 500,
    animationEasing: undefined
  },
  'grid': {
    name: 'grid',
    fit: true,
    padding: 30,
    avoidOverlap: true,
    avoidOverlapPadding: 10,
    nodeDimensionsIncludeLabels: false,
    spacingFactor: undefined,
    condense: false,
    rows: undefined,
    cols: undefined,
    position: function( node ){},
    sort: undefined,
    animate: false,
    animationDuration: 500,
    animationEasing: undefined
  }
}

// Cytoscape 样式
const cytoscapeStyle = [
  {
    selector: 'node',
    style: {
      // 优先显示第一个非系统属性
      'label': function(ele) {
        const data = ele.data()

        if (data.name) {
          return data.name
        }
        // 获取所有属性键
        const keys = Object.keys(data)
        // 过滤掉系统属性
        const systemProps = ['id', 'label', 'source', 'target', '@type', '@value']
        const userProps = keys.filter(k => !systemProps.includes(k))
        
        // 优先显示第一个用户属性的值
        if (userProps.length > 0) {
          const firstProp = userProps[0]
          return data[firstProp] || data.id || data.label || ''
        }
        
        // 如果没有用户属性，显示id或label
        return data.id || data.label || ''
      },
      'text-valign': 'top',
      'text-halign': 'center',
      'text-margin-y': -10,
      'background-color': '#667eea',
      'color': '#333',
      'font-size': '18px',
      'font-weight': 'bold',
      // 增大节点尺寸到 120px
      'width': '120px',
      'height': '120px',
      'text-outline-width': 3,
      'text-outline-color': '#ffffff',
      'overlay-padding': '10px',
      'z-index': 10,
      'border-width': 4,
      'border-color': '#ffffff',
      // 阴影效果
      'shadow-blur': 15,
      'shadow-color': '#00000040',
      'shadow-offset-x': 3,
      'shadow-offset-y': 3
    }
  },
  {
    selector: 'node:selected',
    style: {
      'background-color': '#ff6b6b',
      'border-width': 4,
      'border-color': '#ff3838',
      'text-outline-color': '#ffffff',
      'shadow-blur': 20,
      'shadow-color': '#ff6b6b88'
    }
  },
  {
    selector: 'node[label="person"]',
    style: {
      'background-color': '#667eea',
      'shape': 'ellipse'
    }
  },
  {
    selector: 'edge',
    style: {
      'width': 4,  // 增加边宽度
      'line-color': '#999',
      'target-arrow-color': '#999',
      'target-arrow-shape': 'triangle',
      'target-arrow-size': 12,  // 增大箭头
      // 使用 bezier 或 unbundled-bezier 避免重叠
      'curve-style': 'unbundled-bezier',
      'control-point-distances': [40, -40],
      'control-point-weights': [0.25, 0.75],
      // 边标签
      'label': 'data(label)',
      'font-size': '13px',  // 增大字体
      'text-rotation': 'autorotate',
      'text-margin-y': -12,
      'text-background-color': '#ffffff',
      'text-background-opacity': 0.9,
      'text-background-padding': '4px',
      'text-background-shape': 'roundrectangle',
      'color': '#666'
    }
  },
  {
    selector: 'edge:selected',
    style: {
      'line-color': '#ff6b6b',
      'target-arrow-color': '#ff6b6b',
      'width': 4,
      'color': '#ff6b6b'
    }
  },
  {
    selector: 'edge[label="knows"]',
    style: {
      'line-color': '#4ecdc4',
      'target-arrow-color': '#4ecdc4'
    }
  }
]

// 初始化 Cytoscape
const initCytoscape = () => {
  if (!cyContainer.value) return

  cy = cytoscape({
    container: cyContainer.value,
    elements: [],
    style: cytoscapeStyle,
    layout: layouts[currentLayout.value],
    minZoom: 0.1,
    maxZoom: 3,
    wheelSensitivity: 2.0  // 提升到4x速度（从0.5提升）
  })

  // 创建 tooltip 元素
  let tooltipDiv = document.getElementById('cy-tooltip')
  if (!tooltipDiv) {
    tooltipDiv = document.createElement('div')
    tooltipDiv.id = 'cy-tooltip'
    tooltipDiv.style.cssText = `
      position: absolute;
      display: none;
      background: rgba(0, 0, 0, 0.85);
      color: white;
      padding: 8px 12px;
      border-radius: 6px;
      font-size: 12px;
      pointer-events: none;
      z-index: 10000;
      max-width: 300px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    `
    document.body.appendChild(tooltipDiv)
  }

  // 格式化属性为 HTML
  const formatProperties = (data) => {
    const lines = []
    for (const [key, value] of Object.entries(data)) {
      // 跳过内部属性
      if (key === 'source' || key === 'target') continue
      
      let displayValue = value
      if (typeof value === 'object') {
        displayValue = JSON.stringify(value)
      }
      
      lines.push(`<div style="margin: 2px 0;"><strong>${key}:</strong> ${displayValue}</div>`)
    }
    return lines.join('')
  }

  // 节点和边的 hover 事件
  cy.on('mouseover', 'node', (evt) => {
    const node = evt.target
    const data = node.data()
    const pos = evt.renderedPosition
    
    tooltipDiv.innerHTML = `
      <div style="font-weight: bold; margin-bottom: 5px; border-bottom: 1px solid #fff; padding-bottom: 3px;">
        Node: ${data.name || data.id || 'Unknown'}
      </div>
      ${formatProperties(data)}
    `
    tooltipDiv.style.display = 'block'
    tooltipDiv.style.left = (pos.x + 15) + 'px'
    tooltipDiv.style.top = (pos.y + 15) + 'px'
  })

  cy.on('mouseover', 'edge', (evt) => {
    const edge = evt.target
    const data = edge.data()
    const pos = evt.renderedPosition
    
    tooltipDiv.innerHTML = `
      <div style="font-weight: bold; margin-bottom: 5px; border-bottom: 1px solid #fff; padding-bottom: 3px;">
        Edge: ${data.label || 'Unknown'}
      </div>
      ${formatProperties(data)}
    `
    tooltipDiv.style.display = 'block'
    tooltipDiv.style.left = (pos.x + 15) + 'px'
    tooltipDiv.style.top = (pos.y + 15) + 'px'
  })

  cy.on('mouseout', 'node, edge', () => {
    tooltipDiv.style.display = 'none'
  })

  // 节点移动时更新 tooltip 位置
  cy.on('mousemove', 'node, edge', (evt) => {
    const pos = evt.renderedPosition
    tooltipDiv.style.left = (pos.x + 15) + 'px'
    tooltipDiv.style.top = (pos.y + 15) + 'px'
  })

  // 绑定点击事件
  cy.on('tap', 'node', (evt) => {
    const node = evt.target
    selectedNode.value = node.data()
  })

  cy.on('tap', (evt) => {
    if (evt.target === cy) {
      clearSelection()
    }
  })

  // 更新图数据
  updateGraph()
}

// 根据节点数量计算合适的节点大小
const calculateNodeSize = (nodeCount) => {
  if (nodeCount <= 0) return 120
  
  // 动态调整节点大小
  // 节点越多，节点越小
  if (nodeCount <= 5) return 140      // 5个以下：很大
  if (nodeCount <= 10) return 120     // 6-10个：大
  if (nodeCount <= 20) return 100     // 11-20个：中等
  if (nodeCount <= 50) return 80      // 21-50个：较小
  if (nodeCount <= 100) return 60     // 51-100个：小
  return 50                            // 100+个：很小
}

// 更新图数据
const updateGraph = () => {
  if (!cy) return

  const elements = [
    ...store.graphData.nodes,
    ...store.graphData.edges
  ]

  console.log(`Graph updated: ${store.graphData.nodes.length} nodes, ${store.graphData.edges.length} edges`)
  console.log('Nodes:', store.graphData.nodes)
  console.log('Edges:', store.graphData.edges)
  console.log('Elements:', elements)

  // 计算合适的节点大小
  const nodeSize = calculateNodeSize(store.graphData.nodes.length)
  const fontSize = Math.max(12, Math.floor(nodeSize * 0.15))  // 字体大小约为节点的15%
  
  // 动态更新节点样式
  cy.style()
    .selector('node')
    .style({
      'width': `${nodeSize}px`,
      'height': `${nodeSize}px`,
      'font-size': `${fontSize}px`
    })
    .update()

  cy.elements().remove()
  cy.add(elements)
  
  console.log('Cytoscape elements after add:', cy.elements().length, 'total')
  console.log('Cytoscape nodes:', cy.nodes().length)
  console.log('Cytoscape edges:', cy.edges().length)
  
  // 重新应用布局
  const layout = cy.layout(layouts[currentLayout.value])
  
  // 布局完成后自动居中
  layout.on('layoutstop', () => {
    cy.fit(undefined, 70)
    cy.center()
  })
  
  layout.run()
}

// 适应视图
const fitGraph = () => {
  if (cy) {
    cy.fit(undefined, 70)  // 减小padding，让图更充实
  }
}

// 重置缩放
const resetZoom = () => {
  if (cy) {
    cy.fit(undefined, 70)  // 先适配窗口
    cy.center()            // 然后居中
  }
}

// 切换布局
const changeLayout = () => {
  const layoutNames = Object.keys(layouts)
  const currentIndex = layoutNames.indexOf(currentLayout.value)
  const nextIndex = (currentIndex + 1) % layoutNames.length
  currentLayout.value = layoutNames[nextIndex]
  
  if (cy) {
    cy.layout(layouts[currentLayout.value]).run()
  }
}

// 清除选择
const clearSelection = () => {
  selectedNode.value = null
  if (cy) {
    cy.elements().unselect()
  }
}

// 监听图数据变化
watch(() => store.graphData, () => {
  if (cy) {
    updateGraph()
  }
}, { deep: true })

// 挂载时初始化
onMounted(async () => {
  await nextTick()
  initCytoscape()
})
</script>

<style scoped lang="scss">
.graph-visualization {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;

  .graph-toolbar {
    padding: 15px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fafafa;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }

  .cy-container {
    flex: 1;
    width: 100%;
    background: #fafafa;
    position: relative;
  }

  .node-details {
    position: absolute;
    right: 20px;
    top: 80px;
    width: 300px;
    max-height: 500px;
    overflow-y: auto;
    z-index: 1000;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>

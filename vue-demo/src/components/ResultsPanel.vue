<template>
  <div class="results-panel">
    <div class="panel-header">
      <h3>
        <el-icon><DocumentCopy /></el-icon>
        Query Results
      </h3>
      <el-button 
        type="text" 
        @click="closePanel"
        size="small"
      >
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="panel-content" v-if="store.queryResult">
      <!-- 查询信息 -->
      <el-alert
        :type="store.queryResult.success ? 'success' : 'error'"
        :closable="false"
        class="result-alert"
      >
        <template #title>
          <div class="alert-content">
            <span v-if="store.queryResult.success">
              ✓ Query executed successfully
            </span>
            <span v-else>
              ✗ Query failed
            </span>
            <el-tag 
              v-if="store.queryResult.executionTime" 
              size="small"
              type="info"
            >
              {{ store.queryResult.executionTime }}ms
            </el-tag>
          </div>
        </template>
      </el-alert>

      <!-- 错误信息 -->
      <el-alert
        v-if="!store.queryResult.success && store.queryResult.error"
        type="error"
        :title="store.queryResult.error"
        :closable="false"
        class="error-alert"
      />

      <!-- 结果数据 -->
      <div v-if="store.queryResult.success && store.queryResult.data">
        <el-tabs v-model="activeTab" class="result-tabs">
          <!-- 表格视图 -->
          <el-tab-pane label="Table" name="table">
            <div class="result-table">
              <div class="result-summary">
                <el-text type="info" size="small">
                  {{ resultCount }} result(s)
                </el-text>
              </div>
              
              <el-scrollbar height="500px">
                <div v-if="isTableData" class="table-container">
                  <el-table 
                    :data="tableData" 
                    size="small"
                    stripe
                    border
                  >
                    <el-table-column
                      v-for="col in tableColumns"
                      :key="col"
                      :prop="col"
                      :label="col"
                      min-width="120"
                    >
                      <template #default="{ row }">
                        <span class="cell-value">{{ formatValue(row[col]) }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                <div v-else class="list-container">
                  <el-tag
                    v-for="(item, index) in store.queryResult.data"
                    :key="index"
                    class="result-tag"
                    type="info"
                  >
                    {{ formatValue(item) }}
                  </el-tag>
                </div>
              </el-scrollbar>
            </div>
          </el-tab-pane>

          <!-- JSON 视图 -->
          <el-tab-pane label="JSON" name="json">
            <div class="json-view">
              <el-button 
                size="small" 
                @click="copyToClipboard"
                class="copy-button"
              >
                <el-icon><CopyDocument /></el-icon>
                Copy
              </el-button>
              <el-scrollbar height="500px">
                <pre class="json-content">{{ formattedJSON }}</pre>
              </el-scrollbar>
            </div>
          </el-tab-pane>

          <!-- 统计视图 -->
          <el-tab-pane label="Stats" name="stats">
            <div class="stats-view">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="Result Count">
                  {{ resultCount }}
                </el-descriptions-item>
                <el-descriptions-item label="Result Type">
                  {{ resultType }}
                </el-descriptions-item>
                <el-descriptions-item label="Execution Time">
                  {{ store.queryResult.executionTime }}ms
                </el-descriptions-item>
                <el-descriptions-item label="Query">
                  <pre class="query-text">{{ store.queryResult.query }}</pre>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useGraphStore } from '@/store/graphStore'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Close, CopyDocument } from '@element-plus/icons-vue'

const store = useGraphStore()
const activeTab = ref('json')  // 默认显示JSON格式

// 结果数量
const resultCount = computed(() => {
  if (!store.queryResult?.data) return 0
  return Array.isArray(store.queryResult.data) 
    ? store.queryResult.data.length 
    : 1
})

// 结果类型
const resultType = computed(() => {
  if (!store.queryResult?.data) return 'Unknown'
  const data = store.queryResult.data
  
  if (Array.isArray(data)) {
    if (data.length === 0) return 'Empty Array'
    if (typeof data[0] === 'object') return 'Array of Objects'
    return 'Array of Primitives'
  }
  
  return typeof data
})

// 判断是否为表格数据
const isTableData = computed(() => {
  const data = store.queryResult?.data
  if (!Array.isArray(data) || data.length === 0) return false
  
  // 检查第一个元素是否为对象
  return typeof data[0] === 'object' && data[0] !== null
})

// 表格列
const tableColumns = computed(() => {
  if (!isTableData.value) return []
  
  const firstItem = store.queryResult.data[0]
  if (firstItem instanceof Map) {
    return Array.from(firstItem.keys())
  }
  return Object.keys(firstItem)
})

// 表格数据
const tableData = computed(() => {
  if (!isTableData.value) return []
  
  return store.queryResult.data.map((item, index) => {
    if (item instanceof Map) {
      const obj = { _index: index }
      for (const [key, value] of item.entries()) {
        obj[key] = value
      }
      return obj
    }
    return { _index: index, ...item }
  })
})

// 格式化 JSON
const formattedJSON = computed(() => {
  if (!store.queryResult?.data) return ''
  
  try {
    // 处理 Map 对象
    const data = store.queryResult.data
    if (Array.isArray(data)) {
      const converted = data.map(item => {
        if (item instanceof Map) {
          return Object.fromEntries(item)
        }
        return item
      })
      return JSON.stringify(converted, null, 2)
    }
    return JSON.stringify(data, null, 2)
  } catch (error) {
    return String(store.queryResult.data)
  }
})

// 格式化值
const formatValue = (value) => {
  if (value === null || value === undefined) return 'null'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// 复制到剪贴板
const copyToClipboard = () => {
  navigator.clipboard.writeText(formattedJSON.value)
    .then(() => {
      ElMessage.success('Copied to clipboard')
    })
    .catch(() => {
      ElMessage.error('Failed to copy')
    })
}

// 关闭面板
const closePanel = () => {
  store.clearQueryResult()
}
</script>

<style scoped lang="scss">
.results-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .panel-header {
    padding: 15px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fafafa;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .panel-content {
    flex: 1;
    overflow-y: auto;
    padding: 15px;

    .result-alert {
      margin-bottom: 15px;

      .alert-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }

    .error-alert {
      margin-bottom: 15px;
    }

    .result-tabs {
      :deep(.el-tabs__content) {
        padding: 0;
      }
    }

    .result-table {
      .result-summary {
        margin-bottom: 10px;
        padding: 8px;
        background: #f5f7fa;
        border-radius: 4px;
      }

      .table-container {
        .cell-value {
          font-size: 13px;
          font-family: 'Courier New', monospace;
        }
      }

      .list-container {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        padding: 10px;

        .result-tag {
          font-family: 'Courier New', monospace;
        }
      }
    }

    .json-view {
      position: relative;

      .copy-button {
        position: absolute;
        top: 10px;
        right: 10px;
        z-index: 10;
      }

      .json-content {
        padding: 15px;
        background: #f5f7fa;
        border-radius: 4px;
        font-size: 12px;
        font-family: 'Courier New', monospace;
        overflow-x: auto;
        margin: 0;
      }
    }

    .stats-view {
      padding: 15px;

      .query-text {
        font-size: 12px;
        font-family: 'Courier New', monospace;
        margin: 0;
        padding: 8px;
        background: #f5f7fa;
        border-radius: 4px;
        max-height: 200px;
        overflow-y: auto;
      }
    }
  }
}
</style>

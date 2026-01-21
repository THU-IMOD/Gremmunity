<template>
  <div class="query-editor">
    <div class="editor-header">
      <h3>
        <el-icon><Edit /></el-icon>
        Query Editor
      </h3>
      <div class="editor-actions">
        <el-button 
          type="primary" 
          @click="executeQuery"
          :loading="store.isExecuting"
          :disabled="!store.isConnected || !localQuery.trim()"
        >
          <el-icon><CaretRight /></el-icon>
          Execute
        </el-button>
        <el-button 
          @click="clearQuery"
          :disabled="!localQuery"
        >
          <el-icon><Delete /></el-icon>
          Clear
        </el-button>
      </div>
    </div>

    <div class="editor-container">
      <el-input
        v-model="localQuery"
        type="textarea"
        :rows="6"
        :placeholder="placeholderText"
        class="query-textarea"
        @keydown.ctrl.enter="executeQuery"
      />
      <div class="editor-footer">
        <el-text size="small" type="info">
          Press Ctrl+Enter to execute
        </el-text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useGraphStore } from '@/store/graphStore'
import { Edit, CaretRight, Delete } from '@element-plus/icons-vue'

const store = useGraphStore()
const emit = defineEmits(['execute'])

// 本地查询文本
const localQuery = ref('')

// Placeholder 文本
const placeholderText = `Enter your Gremmunity query here...

Example:
g.V().valueMap(true).toList()
g.secondOrder().forall('x').exist('y').filter('g.V(x).out("knows").is(y)').execute()`

// 监听 store 中的 currentQuery 变化
watch(() => store.currentQuery, (newQuery) => {
  if (newQuery) {
    localQuery.value = newQuery
  }
})

// 执行查询
const executeQuery = () => {
  if (localQuery.value.trim()) {
    emit('execute', localQuery.value)
  }
}

// 清空查询
const clearQuery = () => {
  localQuery.value = ''
  store.clearQueryResult()
}
</script>

<style scoped lang="scss">
.query-editor {
  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
      color: #303133;
    }

    .editor-actions {
      display: flex;
      gap: 8px;
    }
  }

  .editor-container {
    :deep(.query-textarea) {
      textarea {
        font-family: 'Courier New', Consolas, Monaco, monospace;
        font-size: 14px;
        line-height: 1.6;
      }
    }

    .editor-footer {
      margin-top: 8px;
      text-align: right;
    }
  }
}
</style>

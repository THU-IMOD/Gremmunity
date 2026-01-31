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
      <!-- 新增 autocomplete/off 防止浏览器自动校验 -->
      <el-input
        v-model="localQuery"
        type="textarea"
        :rows="6"
        :placeholder="placeholderText"
        class="query-textarea"
        @keydown.ctrl.enter="executeQuery"
        autocomplete="off"
        autocorrect="off"
        autocapitalize="off"
        spellcheck="false"
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

// Placeholder 文本（修正拼写错误：Gremmunity → Gremlin）
const placeholderText = `Enter your SO-Gremlin query here...

Example:
g.V().valueMap(true).toList()
g.SecondOrder().forall('x').exist('y').filter('g.V(x).out("knows").is(y)').execute()`

// 监听 store 中的 currentQuery 变化（添加 immediate 防止初始值丢失）
watch(() => store.currentQuery, (newQuery) => {
  if (newQuery) {
    localQuery.value = newQuery
  }
}, { immediate: true })

// 执行查询
const executeQuery = () => {
  if (localQuery.value.trim()) {
    emit('execute', localQuery.value)
  }
}

// 清空查询
const clearQuery = () => {
  localQuery.value = ''
  store.clearQueryResult?.() // 可选链防止方法不存在报错
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
        // 强制重置样式，防止校验标红
        border: 1px solid #dcdfe6 !important;
        box-shadow: none !important;
      }
      
      // 禁用 Element Plus 的错误状态样式
      :deep(.el-input__wrapper.is-error) {
        box-shadow: none;
        border-color: #dcdfe6;
      }
    }

    .editor-footer {
      margin-top: 8px;
      text-align: right;
    }
  }
}
</style>
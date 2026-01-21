<template>
  <div class="graph-uploader">
    <el-dialog
      v-model="dialogVisible"
      title="Create New Graph"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="150px"
        label-position="left"
      >
        <!-- Graph Name -->
        <el-form-item label="Graph Name" prop="graphName">
          <el-input
            v-model="formData.graphName"
            placeholder="e.g., example"
            clearable
          >
            <template #prepend>graph =</template>
          </el-input>
          <div class="help-text">
            Name for CommunityGraph.open('{{ formData.graphName || "name" }}')
          </div>
        </el-form-item>

        <!-- Graph Topology File (.graph) -->
        <el-form-item label="Graph Topology">
          <el-upload
            ref="graphUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".graph"
            :on-change="handleGraphFileChange"
            :on-remove="handleGraphFileRemove"
            :file-list="graphFileList"
          >
            <el-button type="primary" :icon="Upload">
              Select .graph File
            </el-button>
            <template #tip>
              <div class="upload-tip">
                Graph topology file (optional - only needed for new graphs)
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- Vertex Properties File -->
        <el-form-item label="Vertex Properties" prop="vertexFile">
          <el-upload
            ref="vertexUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".json,.csv"
            :on-change="handleVertexFileChange"
            :on-remove="handleVertexFileRemove"
            :file-list="vertexFileList"
          >
            <el-button type="primary" :icon="Upload">
              Select .json/.csv File
            </el-button>
            <template #tip>
              <div class="upload-tip">
                Vertex property file (optional, .json or .csv)
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- Edge Properties File -->
        <el-form-item label="Edge Properties" prop="edgeFile">
          <el-upload
            ref="edgeUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".json,.csv"
            :on-change="handleEdgeFileChange"
            :on-remove="handleEdgeFileRemove"
            :file-list="edgeFileList"
          >
            <el-button type="primary" :icon="Upload">
              Select .json/.csv File
            </el-button>
            <template #tip>
              <div class="upload-tip">
                Edge property file (optional, .json or .csv)
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- Preview Generated Code -->
        <el-divider />
        <el-form-item label="Preview (Auto-executed by server)">
          <div class="help-text" style="margin-bottom: 8px; color: #67C23A;">
            ℹ️ The server will automatically execute this initialization code after upload.
            You can query directly without manual initialization.
          </div>
          <el-input
            :model-value="generatedCode"
            type="textarea"
            :rows="8"
            readonly
            class="code-preview"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancel">Cancel</el-button>
          <el-button type="info" @click="handleCopyCode" :icon="DocumentCopy">
            Copy Code
          </el-button>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="uploading"
            :icon="Check"
          >
            Upload
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, DocumentCopy, Check } from '@element-plus/icons-vue'
import { useGraphStore } from '../store/graphStore'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'success'])

const store = useGraphStore()
const formRef = ref(null)
const graphUploadRef = ref(null)
const vertexUploadRef = ref(null)
const edgeUploadRef = ref(null)

// Form data
const formData = ref({
  graphName: '',
  graphFile: null,
  vertexFile: null,
  edgeFile: null
})

// File lists for upload components
const graphFileList = ref([])
const vertexFileList = ref([])
const edgeFileList = ref([])

// Upload state
const uploading = ref(false)

// Dialog visibility
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// Form validation rules
const formRules = {
  graphName: [
    { required: true, message: 'Please enter graph name', trigger: 'blur' },
    { 
      pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
      message: 'Name must start with letter and contain only letters, numbers, underscore, hyphen',
      trigger: 'blur'
    }
  ]
  // graphFile is now optional - you can reload existing graphs
}

// Generate initialization code
const generatedCode = computed(() => {
  if (!formData.value.graphName) {
    return '// Enter graph name to preview initialization code'
  }

  const lines = []
  
  // Add header comment
  lines.push(`// ===================================================`)
  lines.push(`// Auto-executed by server - No manual execution needed`)
  lines.push(`// After upload, you can query directly: g.V().count()`)
  lines.push(`// ===================================================`)
  lines.push('')
  
  // 1. Reload graph
  lines.push(`// 1. Reload graph`)
  lines.push(`graph.reload('${formData.value.graphName}');`)
  lines.push('')
  
  // 2. Initialize traversal source
  lines.push(`// 2. Initialize traversal source`)
  lines.push(`g = graph.traversal(SecondOrderTraversalSource.class);`)
  lines.push('')
  
  // 3. Load vertex properties (if provided)
  if (formData.value.vertexFile) {
    const fileName = formData.value.vertexFile.name
    lines.push(`// 3. Load vertex properties`)
    lines.push(`graph.loadVertexProperty('${fileName}');`)
    lines.push('')
  }
  
  // 4. Load edge properties (if provided)
  if (formData.value.edgeFile) {
    const fileName = formData.value.edgeFile.name
    lines.push(`// 4. Load edge properties`)
    lines.push(`graph.loadEdgeProperty('${fileName}');`)
    lines.push('')
  }
  
  // 5. Success message
  lines.push(`// Graph '${formData.value.graphName}' loaded successfully`)
  lines.push(`'Graph initialized'`)
  
  return lines.join('\n')
})

// File change handlers
const handleGraphFileChange = (file) => {
  formData.value.graphFile = file.raw
  graphFileList.value = [file]
}

const handleGraphFileRemove = () => {
  formData.value.graphFile = null
  graphFileList.value = []
}

const handleVertexFileChange = (file) => {
  formData.value.vertexFile = file.raw
  vertexFileList.value = [file]
}

const handleVertexFileRemove = () => {
  formData.value.vertexFile = null
  vertexFileList.value = []
}

const handleEdgeFileChange = (file) => {
  formData.value.edgeFile = file.raw
  edgeFileList.value = [file]
}

const handleEdgeFileRemove = () => {
  formData.value.edgeFile = null
  edgeFileList.value = []
}

// Handle copy code
const handleCopyCode = async () => {
  try {
    await navigator.clipboard.writeText(generatedCode.value)
    ElMessage.success('Code copied to clipboard')
  } catch (error) {
    ElMessage.error('Failed to copy code')
  }
}

// Handle cancel
const handleCancel = () => {
  dialogVisible.value = false
  resetForm()
}

// Reset form
const resetForm = () => {
  formRef.value?.resetFields()
  formData.value = {
    graphName: '',
    graphFile: null,
    vertexFile: null,
    edgeFile: null
  }
  graphFileList.value = []
  vertexFileList.value = []
  edgeFileList.value = []
}

// Handle submit
const handleSubmit = async () => {
  try {
    // Validate form
    await formRef.value.validate()
    
    // Confirm upload
    await ElMessageBox.confirm(
      `This will upload files and create graph '${formData.value.graphName}'. Continue?`,
      'Confirm Upload',
      {
        type: 'warning',
        confirmButtonText: 'Upload',
        cancelButtonText: 'Cancel'
      }
    )
    
    uploading.value = true
    
    try {
      // Upload files
      await uploadFiles()
      
      // Server automatically executes initialization
      // No need to execute it again from frontend
      
      ElMessage.success({
        message: 'Graph uploaded successfully! Server has automatically initialized the graph. You can query directly.',
        duration: 5000
      })
      
      emit('success', {
        graphName: formData.value.graphName,
        code: generatedCode.value
      })
      
      dialogVisible.value = false
      resetForm()
    } catch (error) {
      console.error('Upload failed:', error)
      ElMessage.error(error.message || 'Failed to upload graph')
    } finally {
      uploading.value = false
    }
  } catch (error) {
    // Handle validation or confirmation cancel
    if (error !== 'cancel') {
      console.error('Error:', error)
    }
  }
}

// Upload files to server
const uploadFiles = async () => {
  const formDataToSend = new FormData()
  
  // Add graph name
  formDataToSend.append('graphName', formData.value.graphName)
  
  // Add files (all optional now)
  if (formData.value.graphFile) {
    formDataToSend.append('graphFile', formData.value.graphFile)
  }
  
  if (formData.value.vertexFile) {
    formDataToSend.append('vertexFile', formData.value.vertexFile)
  }
  
  if (formData.value.edgeFile) {
    formDataToSend.append('edgeFile', formData.value.edgeFile)
  }
  
  // Send to server
  const response = await fetch('http://localhost:8284/api/graph/upload', {
    method: 'POST',
    body: formDataToSend
  })
  
  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Upload failed')
  }
  
  return await response.json()
}

// Watch dialog visibility to reset form when closed
watch(dialogVisible, (visible) => {
  if (!visible) {
    resetForm()
  }
})
</script>

<style scoped lang="scss">
.graph-uploader {
  .help-text {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
  
  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
  
  .code-preview {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    
    :deep(textarea) {
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      background-color: #f5f7fa;
      color: #303133;
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>

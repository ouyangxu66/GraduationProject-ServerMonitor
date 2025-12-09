<template>
  <div class="server-page">
    <!-- 顶部操作栏：扁平化设计 -->
    <div class="action-bar">
      <div class="title-box">
        <h2 class="page-title">服务器列表</h2>
        <span class="page-desc">管理您的 Linux 服务器资源</span>
      </div>
      <el-button type="primary" size="large" class="add-btn" @click="openDialog()">
        <el-icon style="margin-right: 5px"><Plus /></el-icon>
        新增服务器
      </el-button>
    </div>

    <!-- 表格区域：去除阴影，使用粗线条边框 -->
    <div class="table-container">
      <el-table
          :data="tableData"
          style="width: 100%"
          :header-cell-style="{ background: '#f8f9fa', color: '#2c3e50', fontWeight: '800' }"
          border
          stripe
      >
        <el-table-column prop="name" label="服务器名称" min-width="150">
          <template #default="{ row }">
            <span class="server-name">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP 地址" width="160" />

        <el-table-column prop="port" label="SSH 端口" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" class="port-tag">{{ row.port }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="username" label="用户名" width="120" />

        <el-table-column prop="createTime" label="添加时间" width="180" />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <!-- 扁平化操作按钮组 -->
            <el-button type="success" size="small" class="flat-action-btn" @click="handleConnect(scope.row)">
              <el-icon><Monitor /></el-icon> 终端
            </el-button>
            <el-button type="primary" size="small" class="flat-action-btn" @click="openDialog(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="danger" size="small" class="flat-action-btn" @click="handleDelete(scope.row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 弹窗：扁平化表单 -->
    <el-dialog
        v-model="dialogVisible"
        :title="form.id ? '编辑服务器' : '新增服务器'"
        width="480px"
        align-center
        class="flat-dialog"
    >
      <el-form :model="form" label-width="80px" size="large" class="dialog-form">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：Web-Server-01" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model.number="form.port" type="number" placeholder="22" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="root" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="SSH 密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="submitForm" size="large" class="confirm-btn">确定保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getServerList, saveServer, deleteServer } from '@/api/monitor.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Monitor, Edit, Delete } from '@element-plus/icons-vue'

const router = useRouter()
const tableData = ref([])
const dialogVisible = ref(false)

// 表单数据
const form = reactive({
  id: null,
  name: '',
  ip: '',
  port: 22,
  username: 'root',
  password: ''
})

// 加载列表
const loadList = async () => {
  try {
    const res = await getServerList()
    // 兼容后端返回格式 (可能是 res.data 或直接是 res)
    const list = res.data || res
    if (Array.isArray(list)) {
      tableData.value = list
    }
  } catch (e) {
    console.error(e)
  }
}

// 打开弹窗
const openDialog = (row = null) => {
  if (row) {
    Object.assign(form, row)
  } else {
    form.id = null
    form.name = ''
    form.ip = ''
    form.port = 22
    form.username = 'root'
    form.password = ''
  }
  dialogVisible.value = true
}

// 提交保存
const submitForm = async () => {
  if(!form.ip || !form.username) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    await saveServer(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 错误已由 request.js 处理
  }
}

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这台服务器吗？', '系统提示', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteServer(id)
      ElMessage.success('删除成功')
      loadList()
    } catch (e) {}
  })
}

// 连接跳转
const handleConnect = (row) => {
  router.push({
    path: '/ssh',
    query: {
      ip: row.ip,
      port: row.port,
      user: row.username,
      pwd: row.password
    }
  })
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.server-page {
  /* 页面不设背景色，由 MainLayout 统一控制 */
}

/* 顶部操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 24px;
  border: 2px solid #ecf0f1; /* 扁平化粗边框 */
  border-radius: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #2c3e50;
}

.page-desc {
  font-size: 13px;
  color: #95a5a6;
  margin-top: 4px;
  display: block;
}

.add-btn {
  background-color: #2c3e50; /* 深色按钮 */
  border-color: #2c3e50;
  font-weight: 700;
}
.add-btn:hover {
  background-color: #34495e;
  border-color: #34495e;
}

/* 表格容器 */
.table-container {
  background: #fff;
  padding: 24px;
  border: 2px solid #ecf0f1;
  border-radius: 12px;
}

.server-name {
  font-weight: 700;
  color: #3498db;
}

.port-tag {
  font-family: 'Courier New', monospace;
  font-weight: 700;
}

/* 操作按钮微调 */
.flat-action-btn {
  font-weight: 600;
}

/* 弹窗样式调整 */
.confirm-btn {
  background-color: #3498db;
  border-color: #3498db;
  font-weight: 700;
}
</style>
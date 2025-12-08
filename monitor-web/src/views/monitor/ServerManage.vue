<template>
  <div class="server-container">
    <div class="header">
      <h2>🖥️ 服务器管理</h2>
      <el-button type="primary" @click="openDialog()">+ 新增服务器</el-button>
    </div>

    <!-- 服务器列表表格 -->
    <el-table :data="tableData" style="width: 100%; margin-top: 20px" border>
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="ip" label="IP地址" width="150" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="username" label="用户名" width="100" />
      <el-table-column label="操作">
        <template #default="scope">
          <!-- 🔥 核心功能：连接按钮 -->
          <el-button type="success" size="small" @click="handleConnect(scope.row)">
            🚀 远程连接
          </el-button>
          <el-button type="primary" size="small" @click="openDialog(scope.row)">
            编辑
          </el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑服务器' : '新增服务器'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：阿里云-Web01" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="192.168.x.x" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model.number="form.port" type="number" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getServerList, saveServer, deleteServer } from '@/api/monitor.js'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  const res = await getServerList()
  if (res.data) {
    tableData.value = res.data
  }
}

// 打开弹窗
const openDialog = (row = null) => {
  if (row) {
    // 编辑模式：复制数据
    Object.assign(form, row)
  } else {
    // 新增模式：重置表单
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
  const res = await saveServer(form)
  if (res.data === 'success') {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } else {
    ElMessage.error('保存失败')
  }
}

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' }).then(async () => {
    const res = await deleteServer(id)
    if (res.data === 'success') {
      ElMessage.success('删除成功')
      loadList()
    }
  })
}

// 🔥 核心跳转逻辑
const handleConnect = (row) => {
  // 跳转到 SSH 页面，并通过 Query 参数把 IP/账号/密码 传过去
  // 注意：实际生产中密码不建议直接在 URL 里传，这里为了教学简便
  router.push({
    path: '/ssh',
    query: {
      ip: row.ip,
      user: row.username,
      pwd: row.password, // 这里对应后端 handler 解析的 key
      port: row.port
    }
  })
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.server-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
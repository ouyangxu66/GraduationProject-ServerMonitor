<template>
  <div class="server-page">
    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="title-box">
        <h2 class="page-title">服务器列表</h2>
        <span class="page-desc">管理您的 Linux 主机资源</span>
      </div>
      <el-button type="primary" size="large" class="add-btn" @click="openDialog()">
        <el-icon style="margin-right: 5px"><Plus /></el-icon>
        新增服务器
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table
          :data="tableData"
          style="width: 100%"
          :header-cell-style="{ background: 'var(--el-fill-color-light)', color: 'var(--el-text-color-primary)', fontWeight: '800' }"
          border
          stripe
      >
        <el-table-column prop="name" label="服务器名称" min-width="150">
          <template #default="{ row }">
            <span class="server-name">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP 地址" width="160">
          <template #default="{ row }">
            <el-tag type="info" effect="light">{{ row.ip }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="port" label="端口" width="100" align="center" />

        <el-table-column prop="username" label="用户名" width="120" />

        <el-table-column prop="createTime" label="添加时间" width="180" />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="success" size="small" plain @click="handleConnect(scope.row)">
              <el-icon><Monitor /></el-icon> 终端
            </el-button>
            <el-button type="primary" size="small" plain @click="openDialog(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="danger"
                       size="small"
                       class="flat-action-btn"
                       v-permission="['server:delete']"
                       plain @click="handleDelete(scope.row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="form.id ? '编辑服务器' : '新增服务器'"
        width="480px"
        align-center
        destroy-on-close
    >
      <el-form :model="form" label-width="80px" size="large" class="dialog-form">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：Web-01" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="192.168.x.x" />
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
          <el-button type="primary" @click="submitForm" size="large">确定</el-button>
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

const form = reactive({
  id: null,
  name: '',
  ip: '',
  port: 22,
  username: 'root',
  password: ''
})

const loadList = async () => {
  try {
    const res = await getServerList()
    tableData.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    console.log(e)
  }
}

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

const submitForm = async () => {
  if(!form.ip || !form.username) return ElMessage.warning('请填写必填项')
  try {
    await saveServer(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 错误已由 request.js 处理
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除吗？', '警告', { type: 'warning' }).then(async () => {
    try {
      await deleteServer(id)
      ElMessage.success('删除成功')
      loadList()
    } catch (e) {
      console.log(e)
    }
  })
}

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

onMounted(() => loadList())
</script>

<style scoped>
/* 使用 CSS 变量适配暗黑模式 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: var(--el-bg-color);
  padding: 24px;
  border: 2px solid var(--el-border-color-light);
  border-radius: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

.page-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  display: block;
}

.table-container {
  background: var(--el-bg-color);
  padding: 24px;
  border: 2px solid var(--el-border-color-light);
  border-radius: 12px;
}

.server-name {
  font-weight: 700;
  color: var(--el-color-primary);
}
</style>
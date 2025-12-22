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

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <!-- 根据 isOnline 显示不同 Tag -->
            <el-tag v-if="row.isOnline" type="success" effect="dark" round>
              <el-icon class="is-loading" style="margin-right:2px" v-if="false"><Loading /></el-icon>
              在线
            </el-tag>
            <el-tag v-else type="info" effect="plain" round>离线</el-tag>
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
        width="520px"
        align-center
        destroy-on-close
    >
      <el-form :model="form" label-width="90px" size="large" class="dialog-form">
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

        <!-- 密码：编辑时允许留空（表示不修改），也支持一键清空 -->
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="SSH 密码（留空表示不修改）" />
          <div class="helper-line" v-if="form.id">
            <el-button link type="danger" @click="clearPassword">清除密码</el-button>
          </div>
        </el-form-item>

        <!-- 私钥：不回显历史私钥；输入不为空则视为替换 -->
        <el-form-item label="私钥">
          <el-input
            v-model="form.privateKeyEnc"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 8 }"
            placeholder="可选：粘贴 PEM 私钥（留空表示不修改）"
          />
          <div class="helper-line" v-if="form.id">
            <span class="muted">提示：出于安全考虑，不回显已保存的私钥。</span>
            <el-button link type="danger" @click="clearPrivateKey">清除私钥</el-button>
          </div>
        </el-form-item>

        <el-form-item label="口令">
          <el-input v-model="form.keyPassphraseEnc" type="password" show-password placeholder="私钥口令（可选，留空表示不修改）" />
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
import { Plus, Monitor, Edit, Delete, Loading } from '@element-plus/icons-vue'

const router = useRouter()
const tableData = ref([])
const dialogVisible = ref(false)

const form = reactive({
  id: null,
  name: '',
  ip: '',
  port: 22,
  username: 'root',
  password: '',
  // 新增：私钥与口令（为了兼容后端直接以 ServerInfo 入参）
  // 说明：这里字段名与后端 ServerInfo 保持一致；
  //       保存时后端会把“明文私钥/口令”加密后入库。
  privateKeyEnc: '',
  keyPassphraseEnc: ''
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
    // 安全：编辑时不回显私钥/口令（后端 list 已脱敏，这里也显式清空）
    form.privateKeyEnc = ''
    form.keyPassphraseEnc = ''
    // 密码同理：后端 list 已不返回；这里保持为空，表示不修改
    form.password = ''
  } else {
    form.id = null
    form.name = ''
    form.ip = ''
    form.port = 22
    form.username = 'root'
    form.password = ''
    form.privateKeyEnc = ''
    form.keyPassphraseEnc = ''
  }
  dialogVisible.value = true
}

const clearPrivateKey = () => {
  form.privateKeyEnc = ''
  form.keyPassphraseEnc = ''
  // 特殊约定：清除私钥使用一个显式标记字段（后端可识别并清空）
  form.hasPrivateKey = 0
}

const clearPassword = () => {
  form.password = ''
  // 这里不额外打标记，后端会在“更新时密码为空则保留旧值”。
  // 如果你需要“强制清空密码”，可以后端再加 clearPassword 标记。
}

const submitForm = async () => {
  if (!form.ip || !form.username) return ElMessage.warning('请填写必填项')
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
  // 重要：不再把 ip/user/pwd 暴露到 URL 参数里，避免泄露
  router.push({
    path: '/ssh',
    query: {
      serverId: row.id
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

.helper-line {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.muted {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
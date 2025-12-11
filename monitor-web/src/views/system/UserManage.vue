<template>
  <div class="user-manage-page">
    <!-- 搜索与操作栏 -->
    <div class="action-bar">
      <div class="search-box">
        <el-input
            v-model="queryParams.username"
            placeholder="搜索用户名..."
            class="search-input"
            clearable
            @clear="loadList"
            @keyup.enter="loadList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>
      <el-button type="primary" class="add-btn" @click="openDialog()">
        <el-icon style="margin-right: 5px"><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <!-- 用户列表表格 -->
    <div class="table-container">
      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="avatar" label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="32" :src="row.avatar">{{ row.username[0] }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ROLE_ADMIN'" type="danger">管理员</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" />

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
            <el-button link type="warning" @click="handleResetPwd(scope.row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-box">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadList"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="普通用户" value="ROLE_USER" />
            <el-option label="超级管理员" value="ROLE_ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input value="123456" disabled placeholder="默认: 123456" />
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
import { ref, reactive, onMounted } from 'vue'
import { getUserList, addUser, updateUser, deleteUser, resetUserPwd } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)

const queryParams = reactive({
  page: 1,
  size: 10,
  username: ''
})

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  role: 'ROLE_USER'
})

// 加载列表
const loadList = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    // 兼容拦截器解包
    const data = res.data || res
    tableData.value = data.records
    total.value = data.total
  } catch(e) {
    console.log(e)
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 弹窗逻辑
const openDialog = (row = null) => {
  if (row) {
    Object.assign(form, row)
  } else {
    form.id = null
    form.username = ''
    form.nickname = ''
    form.role = 'ROLE_USER'
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (form.id) {
      await updateUser(form)
    } else {
      await addUser(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    console.log(e)
    ElMessage.error(e.message || '操作失败')
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗?`, '警告', { type: 'warning' })
      .then(async () => {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        loadList()
      })
}

// 重置密码
const handleResetPwd = (row) => {
  ElMessageBox.confirm(`将用户 "${row.username}" 的密码重置为 123456 ?`, '提示', { type: 'info' })
      .then(async () => {
        await resetUserPwd(row.id)
        ElMessage.success('重置成功')
      })
}

onMounted(() => loadList())
</script>

<style scoped>
.user-manage-page {
  /* 页面样式与 ServerManage 保持一致 */
}
.action-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
}
.search-box {
  display: flex;
  gap: 10px;
}
.search-input {
  width: 240px;
}
.table-container {
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
}
.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
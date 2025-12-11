<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <!-- ====================== 左侧：个人信息卡片 ====================== -->
      <el-col :span="8">
        <div class="flat-card user-card">
          <!-- 头像区域 (已修复重叠问题) -->
          <div class="avatar-wrapper">
            <el-upload
                class="avatar-uploader"
                action="/api/user/avatar"
                :show-file-list="false"
                :headers="uploadHeaders"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
            >
              <!-- 显示头像或占位符 -->
              <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" />
              <div v-else class="avatar-placeholder">{{ userInfo.nickname?.[0] || 'U' }}</div>
              <!-- 悬停显示上传遮罩 -->
              <div class="upload-mask"><el-icon><Camera /></el-icon></div>
            </el-upload>
          </div>

          <!-- 身份信息 -->
          <div class="user-identity">
            <h2 class="nickname">{{ userInfo.nickname || userInfo.username }}</h2>
            <div class="role-badge">{{ userInfo.role === 'ROLE_ADMIN' ? '超级管理员' : '普通用户' }}</div>
          </div>

          <!-- 个人简介 -->
          <div class="bio-section">
            <p>{{ userInfo.bio || '这个人很懒，什么都没写...' }}</p>
          </div>

          <!-- 底部基础信息 -->
          <div class="info-list">
            <div class="info-item">
              <span class="label">账号 ID</span>
              <span class="value">{{ userInfo.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">注册时间</span>
              <span class="value">{{ userInfo.createTime }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- ====================== 右侧：功能编辑区域 ====================== -->
      <el-col :span="16">
        <div class="flat-card form-card">
          <el-tabs v-model="activeTab" class="flat-tabs">

            <!-- Tab 1: 基本资料编辑 -->
            <el-tab-pane label="基本资料" name="info">
              <el-form :model="form" label-position="top" size="large" class="profile-form">
                <el-form-item label="用户昵称">
                  <el-input v-model="form.nickname" :disabled="!isEditing" />
                </el-form-item>
                <el-form-item label="电子邮箱">
                  <el-input v-model="form.email" :disabled="!isEditing" />
                </el-form-item>
                <el-form-item label="个人简介">
                  <el-input
                      type="textarea"
                      :rows="4"
                      v-model="form.bio"
                      placeholder="写点什么..."
                      :disabled="!isEditing"
                      maxlength="200"
                      show-word-limit
                  />
                </el-form-item>

                <!-- 按钮组：切换只读/编辑状态 -->
                <div class="btn-group">
                  <el-button v-if="!isEditing" type="primary" class="action-btn" @click="startEdit">
                    <el-icon style="margin-right:5px"><Edit /></el-icon> 编辑个人资料
                  </el-button>

                  <template v-else>
                    <el-button type="primary" class="action-btn" @click="handleUpdateProfile">保存修改</el-button>
                    <el-button type="danger" class="action-btn" @click="cancelEdit">取消修改</el-button>
                  </template>
                </div>
              </el-form>
            </el-tab-pane>

            <!-- Tab 2: 安全设置 (修改密码 + 注销账号) -->
            <el-tab-pane label="安全设置" name="security">

              <!-- 密码修改流程 - 步骤 1: 验证旧密码 -->
              <div v-if="pwdStep === 1" class="step-container">
                <el-alert title="为了您的账户安全，修改密码前请先验证旧密码" type="info" :closable="false" show-icon style="margin-bottom: 20px"/>
                <el-form :model="pwdForm" label-position="top" size="large" class="profile-form">
                  <el-form-item label="请输入旧密码">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="验证身份" />
                  </el-form-item>
                  <el-button type="primary" class="action-btn" @click="verifyOldPwd" :loading="verifying">下一步</el-button>
                </el-form>
              </div>

              <!-- 密码修改流程 - 步骤 2: 设置新密码 -->
              <div v-else class="step-container">
                <el-alert title="旧密码验证通过，请设置新密码" type="success" :closable="false" show-icon style="margin-bottom: 20px"/>
                <el-form :model="pwdForm" label-position="top" size="large" class="profile-form">
                  <el-form-item label="新密码">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                  </el-form-item>
                  <el-form-item label="确认新密码">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入" />
                  </el-form-item>
                  <div class="btn-group">
                    <el-button type="danger" class="action-btn" @click="handleUpdatePwd">确认重置密码</el-button>
                    <el-button @click="pwdStep = 1" class="action-btn">返回</el-button>
                  </div>
                </el-form>
              </div>

              <!-- 🔴 新增：危险区域 (注销账号) -->
              <div class="divider"></div>
              <div class="danger-zone">
                <div class="danger-header">
                  <el-icon><Warning /></el-icon>
                  <span style="margin-left: 5px">危险区域</span>
                </div>
                <div class="danger-content">
                  <div class="danger-info">
                    <h4>注销账号</h4>
                    <p>一旦注销，您的所有数据将被永久删除，无法恢复。</p>
                  </div>
                  <el-button type="danger" plain @click="handleDeleteAccount">注销</el-button>
                </div>
              </div>

            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
// 引入 API (记得确认 deleteAccount 已在 api/auth.js 中导出)
import { deleteAccount } from '@/api/auth.js'
import { getUserInfo, updateProfile, updatePassword, checkOldPassword } from "@/api/user.js";
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Edit, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from "vue-router"

const userStore = useUserStore()
const router = useRouter()
const activeTab = ref('info')
const userInfo = ref({})

// --- 状态控制 ---
const isEditing = ref(false) // 是否处于编辑模式
const pwdStep = ref(1)       // 密码修改步骤 (1:验旧, 2:改新)
const verifying = ref(false) // 验密Loading

const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

const form = reactive({ nickname: '', email: '', bio: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// ==========================
//  数据加载逻辑
// ==========================
const loadData = async () => {
  const res = await getUserInfo()
  if (res.data || res) {
    const data = res.data || res
    userInfo.value = data
    // 初始化表单数据
    Object.assign(form, data)
  }
}

// ==========================
//  基本资料编辑逻辑
// ==========================
const startEdit = () => {
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  // 恢复原数据
  Object.assign(form, userInfo.value)
  ElMessage.info('已取消修改')
}

const handleUpdateProfile = async () => {
  try {
    await updateProfile(form)
    ElMessage.success('资料更新成功')
    isEditing.value = false
    loadData() // 刷新当前页面数据
    // 🟢 关键：同步刷新全局 Store，让右上角头像同时也变
    userStore.fetchUserInfo()
  } catch (e) {
    // 错误已处理
  }
}

// ==========================
//  密码修改逻辑 (分步验证)
// ==========================
const verifyOldPwd = async () => {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  verifying.value = true
  try {
    // 调用后端校验接口
    const res = await checkOldPassword(pwdForm.oldPassword)
    // 兼容拦截器解包情况
    if (res.code === 200 || res === true) {
      ElMessage.success('验证通过')
      pwdStep.value = 2 // 进入第二步
    }
  } catch (e) {
    const msg = e.message ? e.message.replace('Error: ', '') : '验证失败'
    ElMessage.error(msg || '旧密码验证失败')
  } finally {
    verifying.value = false
  }
}

const handleUpdatePwd = async () => {
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，即将重新登录...')

    // 🟢 关键修复：清除状态并跳转
    userStore.logout()

    setTimeout(() => {
      window.location.reload()
    }, 1500)

  } catch (e) {
    // do nothing
  }
}

// ==========================
//  注销账号逻辑
// ==========================
const handleDeleteAccount = () => {
  ElMessageBox.prompt('请输入登录密码以确认注销', '高风险操作警告', {
    confirmButtonText: '确认注销',
    cancelButtonText: '取消',
    inputType: 'password',
    confirmButtonClass: 'el-button--danger', // 红色确认按钮
    inputPattern: /.+/,
    inputErrorMessage: '密码不能为空',
    type: 'error'
  }).then(async ({ value }) => {
    try {
      await deleteAccount(value)
      ElMessage.success('账号已注销，感谢您的使用')
      userStore.logout()
      setTimeout(() => window.location.reload(), 1500)
    } catch (e) {
      // 错误已处理
    }
  }).catch(() => {})
}

// ==========================
//  头像上传逻辑
// ==========================
const handleAvatarSuccess = (response) => {
  const url = response.data || response
  userInfo.value.avatar = url
  ElMessage.success('头像上传成功')
  // 🟢 关键：同步刷新全局 Store
  userStore.fetchUserInfo()
}

const beforeAvatarUpload = (rawFile) => {
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.profile-container {
  max-width: 1100px;
  margin: 0 auto;
}

.flat-card {
  /* 使用 CSS 变量适配暗黑模式 */
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 30px;
  height: 100%;
}

/* 左侧卡片布局 */
.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 🟢 核心修复：使用 gap 保证间距 */
  gap: 20px;
  padding: 40px 20px;
  height: auto;
  min-height: 100%;
}

/* 🟢 头像容器 (防溢出) */
.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden; /* 裁剪 */
  border: 4px solid var(--el-border-color-lighter);
  flex-shrink: 0;
  position: relative;
  margin: 0;
}

.avatar-uploader {
  width: 100%;
  height: 100%;
  display: block;
}

:deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持比例填充 */
  display: block;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--el-color-primary);
  color: white;
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.upload-mask {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  border-radius: 50%;
}
.avatar-wrapper:hover .upload-mask { opacity: 1; }

/* 昵称区域 */
.user-identity { text-align: center; margin: 0; }
.nickname {
  font-size: 24px;
  color: var(--el-text-color-primary);
  font-weight: 800;
  line-height: 1.4;
  margin: 0 0 8px 0;
}
.role-badge {
  display: inline-block;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.bio-section {
  width: 100%;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 30px;
  padding: 0 10px;
  word-break: break-all;
}

.info-list {
  width: 100%;
  margin-top: auto;
  border-top: 2px solid var(--el-border-color-lighter);
  padding-top: 20px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}
.info-item .label { color: var(--el-text-color-secondary); font-weight: 600; }
.info-item .value { color: var(--el-text-color-primary); font-weight: 700; }

/* 右侧表单样式 */
.profile-form { max-width: 450px; }
.btn-group { margin-top: 30px; display: flex; gap: 15px; }
.action-btn { font-weight: 700; min-width: 120px; }

:deep(.el-input.is-disabled .el-input__wrapper),
:deep(.el-textarea.is-disabled .el-textarea__inner) {
  background-color: var(--el-fill-color-light);
  box-shadow: none;
  border: 1px solid var(--el-border-color-lighter);
}

/* 危险区域样式 (适配暗黑模式) */
.divider {
  height: 1px;
  background-color: var(--el-border-color-lighter);
  margin: 30px 0;
}
.danger-zone {
  border: 1px solid var(--el-color-danger-light-5);
  border-radius: 8px;
  overflow: hidden;
}
.danger-header {
  background-color: var(--el-color-danger-light-9);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  color: var(--el-color-danger);
  font-weight: 700;
}
.danger-content {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.danger-info h4 { margin: 0 0 5px 0; color: var(--el-text-color-primary); }
.danger-info p { margin: 0; font-size: 13px; color: var(--el-text-color-secondary); }

/* 暗黑模式修正 */
html.dark .danger-header {
  background-color: #4a1c1c; /* 深红色背景 */
  color: #ff9999;
}
</style>
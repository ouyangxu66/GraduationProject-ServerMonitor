<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <!-- 左侧：个人卡片 -->
      <el-col :span="8">
        <div class="flat-card user-card">
          <!-- 修复重叠：增加下边距 -->
          <div class="avatar-wrapper">
            <el-upload
                class="avatar-uploader"
                action="/api/user/avatar"
                :show-file-list="false"
                :headers="uploadHeaders"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
            >
              <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" />
              <div v-else class="avatar-placeholder">{{ userInfo.nickname?.[0] || 'U' }}</div>
              <div class="upload-mask"><el-icon><Camera /></el-icon></div>
            </el-upload>
          </div>

          <div class="user-identity">
            <h2 class="nickname">{{ userInfo.nickname || userInfo.username }}</h2>
            <div class="role-badge">{{ userInfo.role === 'ROLE_ADMIN' ? '超级管理员' : '普通用户' }}</div>
          </div>

          <div class="bio-section">
            <p>{{ userInfo.bio || '这个人很懒，什么都没写...' }}</p>
          </div>

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

      <!-- 右侧：功能区域 -->
      <el-col :span="16">
        <div class="flat-card form-card">
          <el-tabs v-model="activeTab" class="flat-tabs">

            <!-- 1. 基本资料 Tab -->
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

            <!-- 2. 安全设置 Tab (分步验证) -->
            <el-tab-pane label="安全设置" name="security">

              <!-- 步骤 1: 验证旧密码 -->
              <div v-if="pwdStep === 1" class="step-container">
                <el-alert title="为了您的账户安全，修改密码前请先验证旧密码" type="info" :closable="false" show-icon style="margin-bottom: 20px"/>
                <el-form :model="pwdForm" label-position="top" size="large" class="profile-form">
                  <el-form-item label="请输入旧密码">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="验证身份" />
                  </el-form-item>
                  <el-button type="primary" class="action-btn" @click="verifyOldPwd" :loading="verifying">下一步</el-button>
                </el-form>
              </div>

              <!-- 步骤 2: 设置新密码 -->
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

            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserInfo, updateProfile, updatePassword, checkOldPassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Camera, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from "vue-router";

const userStore = useUserStore()
const activeTab = ref('info')
const userInfo = ref({})
const router = useRouter()

// --- 状态控制 ---
const isEditing = ref(false) // 是否处于编辑模式
const pwdStep = ref(1)       // 密码修改步骤 (1:验旧, 2:改新)
const verifying = ref(false) // 验密Loading

const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

const form = reactive({ nickname: '', email: '', bio: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 加载数据
const loadData = async () => {
  const res = await getUserInfo()
  if (res.data || res) {
    const data = res.data || res
    userInfo.value = data
    // 初始化表单数据
    Object.assign(form, data)
  }
}

// --- 基本资料逻辑 ---
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
  //   do noting
  }
}

// --- 密码修改逻辑 ---
const verifyOldPwd = async () => {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  verifying.value = true
  try {
    // 调用后端校验接口
    const res = await checkOldPassword(pwdForm.oldPassword)
    if (res.code === 200 || res === true) {
      ElMessage.success('验证通过')
      pwdStep.value = 2 // 进入第二步
    }
  } catch (e) {
    const msg = e.message.replace('Error: ', '')
    ElMessage.error(msg || '旧密码验证失败')  } finally {
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

    // 延迟 1.5秒 让用户看清提示，然后强制刷新页面
    // 使用 location.reload() 会自动触发路由守卫跳转到 /login，且能清空所有内存变量，是最安全的做法
    setTimeout(() => {
      window.location.reload()
    }, 1500)

  } catch (e) {
    //do noting
  }
}

// 头像上传
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
  background: #fff;
  border: 2px solid #ecf0f1;
  border-radius: 16px;
  padding: 30px;
  height: 100%;
}

/* 左侧卡片布局 */
.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 🟢 核心修复：使用 gap 保证间距，无论头像怎么变，昵称都在 20px 之外 */
  gap: 20px;
  padding: 40px 20px;
  height: auto;
  min-height: 100%;
}

/* 🟢 核心修复：头像容器 */
.avatar-wrapper {
  /* 1. 强制固定物理尺寸 */
  width: 120px;
  height: 120px;

  /* 2. 强制圆形裁剪 (关键步骤：超出圆圈的部分直接切掉) */
  border-radius: 50%;
  overflow: hidden;

  /* 3. 边框加在容器上，而不是图片上，防止图片未加载时边框塌陷 */
  border: 4px solid #f0f2f5;

  /* 4. 防止被 Flex 容器压缩 */
  flex-shrink: 0;

  position: relative;
  /* 移除 margin，全靠 gap 控制间距 */
  margin: 0;
}

/* 使得 el-upload 组件填满容器 */
.avatar-uploader {
  width: 100%;
  height: 100%;
  display: block; /* 消除 inline-block 间隙 */
}

/* 覆盖 Element 的内部样式，确保 upload 点击区域填满 */
:deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border: none; /* 移除 Element 可能自带的虚线框 */
}

/* 图片样式 */
.avatar {
  width: 100%;
  height: 100%;
  /* 🟢 核心属性：保持比例填充，多余部分自动裁剪，不会变形 */
  object-fit: cover;
  display: block;
}

/* 占位符样式 */
.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: #3498db;
  color: white;
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* 悬停遮罩 */
.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  border-radius: 50%; /* 遮罩也要圆角 */
}

.avatar-wrapper:hover .upload-mask {
  opacity: 1;
}

/* 昵称区域 */
.user-identity {
  text-align: center;
  /* 移除 margin，由 gap 控制 */
  margin: 0;
}

.nickname {
  font-size: 24px;
  color: #2c3e50;
  font-weight: 800;
  line-height: 1.4;
  margin: 0 0 8px 0; /* 昵称和角色标签之间的小间距 */
}

.role-badge {
  display: inline-block;
  background: #ecf0f1;
  color: #7f8c8d;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.bio-section {
  width: 100%;
  text-align: center;
  color: #7f8c8d;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 30px;
  padding: 0 10px;
  word-break: break-all; /* 防止长文本撑开 */
}

.info-list {
  width: 100%;
  margin-top: auto;
  border-top: 2px solid #f5f7fa;
  padding-top: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.info-item .label { color: #95a5a6; font-weight: 600; }
.info-item .value { color: #2c3e50; font-weight: 700; }

/* 右侧表单样式 */
.profile-form {
  max-width: 450px;
}

.btn-group {
  margin-top: 30px;
  display: flex;
  gap: 15px;
}

.action-btn {
  font-weight: 700;
  min-width: 120px;
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #f9f9f9;
  box-shadow: none;
  border: 1px solid #eee;
}

:deep(.el-textarea.is-disabled .el-textarea__inner) {
  background-color: #f9f9f9;
  box-shadow: none;
  border: 1px solid #eee;
}
</style>
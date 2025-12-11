<template>
  <el-container class="layout-container">
    <!-- 左侧边栏：使用 v-bind 动态控制背景，保持你的浅蓝色设计 -->
    <el-aside width="240px" class="aside">
      <div class="logo">
        <span class="logo-icon">🚀</span>
        <span class="logo-text">Monitor Pro</span>
      </div>

      <el-menu
          :default-active="activeMenu"
          class="flat-menu"
          :background-color="isDark ? '#1d1e1f' : '#98c5e9'"
          :text-color="isDark ? '#cfd3dc' : '#2c3e50'"
          :active-text-color="isDark ? '#409eff' : '#2980b9'"
          router
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>监控大屏</span>
        </el-menu-item>
        <el-menu-item index="/server">
          <el-icon><Service /></el-icon>
          <span>服务器管理</span>
        </el-menu-item>
        <el-menu-item index="/ssh">
          <el-icon><Monitor /></el-icon>
          <span>远程终端</span>
        </el-menu-item>
        <el-menu-item
            index="/userManage"
            v-if="userStore.userInfo.role === 'ROLE_ADMIN'"
        >
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="flat-header">
        <div class="header-left">
          <div class="page-title-box">
            <span class="title-bar"></span>
            <h3 class="page-title">{{ pageTitle }}</h3>
          </div>
        </div>

        <div class="header-right">
          <!-- 🟢 改进后的主题开关：带文字 -->
          <div class="theme-switch" @click="themeStore.toggleDark" :class="{ 'is-dark': isDark }">
            <div class="switch-inner">
              <el-icon v-if="isDark" class="icon-moon"><Moon /></el-icon>
              <el-icon v-else class="icon-sun"><Sunny /></el-icon>
              <span class="switch-text">{{ isDark ? 'Dark' : 'Light' }}</span>
            </div>
          </div>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar
                  :size="36"
                  shape="square"
                  :src="userStore.userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
              />
              <span class="username">{{ userStore.userInfo.nickname || 'Admin' }}</span>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="flat-main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['WebSsh']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { Odometer, Monitor, Service, ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

// 响应式获取 isDark 状态
const { isDark } = storeToRefs(themeStore)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '系统功能')

// 处理命令菜单
const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出当前账户?', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已安全退出')
    }).catch(() => {})
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  userStore.fetchUserInfo()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 🟢 CSS 变量适配区 */
.flat-header {
  /* 使用 Element 变量：亮色时为白色，暗色时为深灰 */
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 64px;
  transition: all 0.3s;
}

.flat-main {
  /* 页面背景色：亮色时为浅灰，暗色时为纯黑 */
  background-color: var(--el-bg-color-page);
  padding: 24px;
  overflow-x: hidden;
  transition: all 0.3s;
}

.title-bar {
  width: 4px;
  height: 18px;
  background-color: var(--el-color-primary);
  margin-right: 10px;
  border-radius: 2px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary); /* 文字颜色自适应 */
  margin: 0;
}

/* 左侧边栏动态配色 */
.aside {
  background-color: v-bind("isDark ? '#1d1e1f' : '#98c5e9'");
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-light);
  transition: background-color 0.3s;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: v-bind("isDark ? '#1d1e1f' : '#98c5e9'");
  border-bottom: 1px solid rgba(0,0,0,0.05);
  transition: background-color 0.3s;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  color: v-bind("isDark ? '#fff' : '#2c3e50'");
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

/* 🟢 主题开关样式优化 (胶囊风格) */
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.theme-switch {
  cursor: pointer;
  background-color: var(--el-fill-color); /* 浅灰底色 */
  padding: 4px 8px;
  border-radius: 20px;
  border: 1px solid var(--el-border-color);
  transition: all 0.3s;
}

.theme-switch:hover {
  border-color: var(--el-color-primary);
}

/* 暗黑模式下开关背景变深 */
.theme-switch.is-dark {
  background-color: #333;
  border-color: #555;
}

.switch-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.switch-text {
  color: var(--el-text-color-regular);
  user-select: none;
}

.icon-sun {
  color: #f1c40f;
  animation: rotate 10s linear infinite;
}

.icon-moon {
  color: #f1c40f;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.user-info:hover {
  background-color: var(--el-fill-color);
}

.username {
  margin-left: 10px;
  margin-right: 4px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
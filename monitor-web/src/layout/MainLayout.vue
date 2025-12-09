<template>
  <el-container class="layout-container">
    <!-- 左侧边栏：浅蓝色风格 -->
    <el-aside width="240px" class="aside">
      <div class="logo">
        <span class="logo-icon">🚀</span>
        <span class="logo-text">Monitor Pro</span>
      </div>

      <el-menu
          :default-active="activeMenu"
          class="flat-menu"
          background-color="#98c5e9"
          text-color="#2c3e50"
          active-text-color="#2980b9"
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
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="flat-header">
        <!-- 左侧：显示当前功能名称 -->
        <div class="header-left">
          <div class="page-title-box">
            <span class="title-bar"></span>
            <h3 class="page-title">{{ pageTitle }}</h3>
          </div>
        </div>

        <!-- 右侧：个人信息与退出 -->
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" shape="square" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">Admin</span>
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

      <!-- 内容区域：增加 Keep-Alive 缓存 -->
      <el-main class="flat-main">
        <router-view v-slot="{ Component }">
          <!-- include="WebSsh" 必须与组件内的 name 属性一致 -->
          <keep-alive :include="['WebSsh']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Odometer, Monitor, Service, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
// 动态获取路由配置中的 meta.title
const pageTitle = computed(() => route.meta.title || '系统功能')

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
    ElMessage.info('个人资料功能开发中...')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 侧边栏：浅蓝色背景 */
.aside {
  background-color: #98c5e9;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #8ab6d9; /* 稍微深一点的边框 */
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #98c5e9;
  border-bottom: 1px solid #8ab6d9;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  color: #2c3e50; /* 深色字体，高对比度 */
}

.flat-menu {
  border-right: none;
  background-color: #98c5e9; /* 确保菜单背景一致 */
}

/* 菜单项样式重写 */
:deep(.el-menu-item) {
  font-weight: 600;
  margin: 4px 10px;
  border-radius: 8px;
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.3) !important; /* 悬停时的半透明白 */
}

/* 菜单选中状态：纯白背景 + 深蓝文字 */
:deep(.el-menu-item.is-active) {
  background-color: #ffffff !important;
  color: #2980b9 !important; /* 选中后的文字颜色 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); /* 极淡的阴影增加层次 */
}

/* 顶部栏 */
.flat-header {
  background-color: #ffffff;
  border-bottom: 1px solid #ecf0f1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title-box {
  display: flex;
  align-items: center;
}

.title-bar {
  width: 4px;
  height: 18px;
  background-color: #3498db;
  margin-right: 10px;
  border-radius: 2px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.user-info:hover {
  background-color: #f0f2f5;
}

.username {
  margin-left: 10px;
  margin-right: 4px;
  font-weight: 600;
  color: #2c3e50;
}

.flat-main {
  background-color: #f9fbfc;
  padding: 24px;
  overflow-x: hidden;
}
</style>
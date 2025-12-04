<script setup>
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <div class="app-layout">
    <!-- 侧边导航栏 -->
    <div class="nav-bar">
      <div class="logo">🚀 Monitor</div>
      <RouterLink to="/" class="nav-item">📊 监控大屏</RouterLink>
      <RouterLink to="/server" class="nav-item">🖥️ 服务器管理</RouterLink>
    </div>

    <!-- 右侧内容区 -->
    <div class="content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
}
.nav-bar {
  width: 200px;
  background-color: #001529; /* 深色背景 */
  color: white;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-weight: bold;
  font-size: 20px;
  border-bottom: 1px solid #333;
}
.nav-item {
  color: #a6adb4;
  text-decoration: none;
  padding: 15px 20px;
  transition: 0.3s;
}
.nav-item:hover, .router-link-active {
  color: white;
  background-color: #1890ff;
}
.content {
  flex: 1;
  background-color: #f0f2f5;
  overflow: auto;
}
</style>
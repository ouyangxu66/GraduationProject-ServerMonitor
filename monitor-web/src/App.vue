<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- RouterView 是路由出口，Dashboard.vue 的内容会显示在这里 -->
  <RouterView />
</template>

<style>
/* 简单的全局样式重置 */
body, html, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}
</style>
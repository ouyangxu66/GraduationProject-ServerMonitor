<template>
  <div class="server-container">
    <el-card shadow="never">
      <div class="header">
        <h2>🖥️ 服务器资源管理</h2>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增服务器</el-button>
      </div>

      <el-table :data="tableData" style="width: 100%; margin-top: 20px" stripe border v-loading="loading">
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="port" label="端口" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" min-width="200">
          <template #default="scope">
            <el-button type="success" size="small" icon="VideoPlay" @click="handleConnect(scope.row)">
              连接终端
            </el-button>
            <el-button type="primary" size="small" icon="Edit" @click="openDialog(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" icon="Delete" @click="handleDelete(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 弹窗 -->
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
          <el-input v-model="form.password" type="password" show-password placeholder
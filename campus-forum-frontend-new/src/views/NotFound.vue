<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.not-found-content {
  text-align: center;

  h1 {
    font-size: 96px;
    margin: 0;
    color: #409eff;
  }

  p {
    font-size: 18px;
    color: #909399;
    margin: 16px 0 32px;
  }
}
</style>

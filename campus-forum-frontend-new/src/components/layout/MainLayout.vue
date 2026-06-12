<template>
  <div class="main-layout">
    <Navbar />
    <div class="main-container">
      <Sidebar class="sidebar" />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import Navbar from './Navbar.vue'
import Sidebar from './Sidebar.vue'
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0, rgba(244, 247, 251, 0) 260px),
    $bg-page;
}

.main-container {
  display: flex;
  flex: 1;
  width: 100%;
  max-width: calc($content-max-width + $sidebar-width + 48px);
  margin: 0 auto;
  padding-top: $navbar-height;
}

.sidebar {
  position: fixed;
  left: 0;
  top: $navbar-height;
  bottom: 0;
  width: $sidebar-width;
  background-color: rgba(255, 255, 255, 0.88);
  border-right: 1px solid $border-light;
  box-shadow: $shadow-xs;
  overflow-y: auto;
  z-index: 100;

  @include mobile {
    display: none;
  }
}

.main-content {
  flex: 1;
  margin-left: $sidebar-width;
  padding: 28px 28px 40px;
  min-width: 0;
  min-height: calc(100vh - $navbar-height);

  @include mobile {
    margin-left: 0;
    padding: 18px 14px 32px;
  }
}
</style>

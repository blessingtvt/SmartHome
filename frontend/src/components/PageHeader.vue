<template>
  <div class="page-head">
    <div class="page-head__left">
      <h2>{{ title }}</h2>
      <p v-if="subtitle" class="page-head__subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-head__right">
      <slot name="extra" />
      <el-tag v-if="mock" type="info" effect="plain" size="small">示例数据 · 待对接</el-tag>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  mock: { type: Boolean, default: false },
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.page-head__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

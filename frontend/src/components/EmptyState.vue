<template>
  <div class="empty-state">
    <el-empty :image-size="imageSize" :description="description">
      <template v-if="$slots.default" #default>
        <slot />
      </template>
    </el-empty>
    <p v-if="hint" class="empty-state__hint">{{ hint }}</p>
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  hint: { type: String, default: '' },
  imageSize: { type: Number, default: 120 },
})
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
  text-align: center;
}

.empty-state__hint {
  margin-top: -8px;
  font-size: 13px;
  color: #909399;
}
</style>

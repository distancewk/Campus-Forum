<template>
  <div class="rich-editor">
    <div class="editor-toolbar" @mousedown.prevent>
      <el-button-group>
        <el-button :icon="Top" @click="formatBlock('h2')" />
        <el-button :icon="Tickets" @click="formatBlock('h3')" />
        <el-button class="format-button" title="加粗" @click="exec('bold')"><strong>B</strong></el-button>
        <el-button class="format-button" title="斜体" @click="exec('italic')"><em>I</em></el-button>
        <el-button class="format-button" title="下划线" @click="exec('underline')"><u>U</u></el-button>
      </el-button-group>
      <el-button-group>
        <el-button :icon="List" @click="exec('insertUnorderedList')" />
        <el-button :icon="Sort" @click="exec('insertOrderedList')" />
      </el-button-group>
      <el-button-group>
        <el-button :icon="Link" @click="insertLink" />
        <el-button :icon="Picture" @click="handleImageUpload" />
        <el-button :icon="Brush" @click="exec('removeFormat')" />
      </el-button-group>
    </div>

    <div
      ref="editorRef"
      class="editor-content"
      contenteditable="true"
      data-placeholder="请输入内容..."
      @input="handleInput"
      @paste="handlePaste"
      @blur="handleInput"
    ></div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import DOMPurify from 'dompurify'
import { Brush, Link, List, Picture, Sort, Tickets, Top } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/post'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
let syncingFromParent = false

const sanitize = (html) => DOMPurify.sanitize(html || '', {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h2', 'h3',
    'ul', 'ol', 'li', 'blockquote', 'a', 'img'
  ],
  ALLOWED_ATTR: ['href', 'target', 'rel', 'src', 'alt', 'width', 'height'],
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|\/uploads\/)/i
})

const normalizeHtml = (html) => sanitize(html).trim()

const updateEditorHtml = async (html) => {
  await nextTick()
  if (!editorRef.value) return
  syncingFromParent = true
  editorRef.value.innerHTML = normalizeHtml(html)
  syncingFromParent = false
}

const emitContent = () => {
  if (!editorRef.value || syncingFromParent) return
  emit('update:modelValue', normalizeHtml(editorRef.value.innerHTML))
}

const focusEditor = () => {
  editorRef.value?.focus()
}

const exec = (command, value = null) => {
  focusEditor()
  document.execCommand(command, false, value)
  emitContent()
}

const formatBlock = (tagName) => {
  exec('formatBlock', tagName)
}

const insertLink = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      confirmButtonText: '插入',
      cancelButtonText: '取消',
      inputPattern: /^https?:\/\/.+/i,
      inputErrorMessage: '链接必须以 http:// 或 https:// 开头'
    })
    exec('createLink', value)
    const selection = window.getSelection()
    const anchor = selection?.anchorNode?.parentElement
    if (anchor?.tagName === 'A') {
      anchor.setAttribute('target', '_blank')
      anchor.setAttribute('rel', 'noopener noreferrer')
    }
    emitContent()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('插入链接失败:', error)
    }
  }
}

async function handleImageUpload() {
  const input = document.createElement('input')
  input.setAttribute('type', 'file')
  input.setAttribute('accept', 'image/jpeg,image/png,image/gif,image/webp')
  input.click()

  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return

    const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
    const isLt5M = file.size / 1024 / 1024 < 5

    if (!isImage) {
      ElMessage.error('只能上传图片文件')
      return
    }
    if (!isLt5M) {
      ElMessage.error('图片大小不能超过5MB')
      return
    }

    try {
      const res = await uploadImage(file)
      const url = res.data?.url
      if (!url) {
        ElMessage.error('图片上传失败')
        return
      }
      exec('insertImage', url)
    } catch (error) {
      ElMessage.error('图片上传失败')
    }
  }
}

const handleInput = () => {
  emitContent()
}

const handlePaste = (event) => {
  event.preventDefault()
  const html = event.clipboardData?.getData('text/html')
  const text = event.clipboardData?.getData('text/plain')
  document.execCommand('insertHTML', false, html ? sanitize(html) : text)
  emitContent()
}

watch(() => props.modelValue, (newVal) => {
  if (editorRef.value && normalizeHtml(newVal) !== normalizeHtml(editorRef.value.innerHTML)) {
    updateEditorHtml(newVal)
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.rich-editor {
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background-color: #fff;
  overflow: hidden;
  box-shadow: $shadow-xs;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid $border-light;
  background-color: $bg-soft;

  :deep(.el-button) {
    min-width: 34px;
    height: 34px;
    border-radius: $radius-sm;
  }
}

.format-button {
  min-width: 32px;
}

.editor-content {
  min-height: 260px;
  padding: 18px 20px;
  line-height: 1.75;
  outline: none;
  color: $text-primary;

  &:empty::before {
    content: attr(data-placeholder);
    color: $text-placeholder;
    pointer-events: none;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
  }

  :deep(a) {
    color: $primary-color;
  }
}

@include mobile {
  .editor-content {
    min-height: 220px;
    padding: 14px;
  }
}
</style>

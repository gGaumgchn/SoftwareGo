<template>
  <div class="file-upload">
    <button
      class="upload"
      :class="{
        done: Boolean(fileId) && !uploading,
        uploading,
        err: Boolean(errorMessage),
      }"
      type="button"
      :disabled="disabled || uploading"
      @click="openPicker"
    >
      <BaseIcon :name="fileId ? 'file' : 'upload'" :size="24" />
      <span>{{ displayText }}</span>
      <small v-if="!uploading && !fileId">{{ hintText }}</small>
    </button>

    <input
      ref="fileInput"
      class="file-input"
      type="file"
      accept=".pdf,application/pdf"
      :disabled="disabled || uploading"
      @change="handleFileChange"
    />

    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import BaseIcon from './BaseIcon.vue';
import { uploadResume } from '../api/file';

const MAX_FILE_SIZE = 10 * 1024 * 1024;

const props = defineProps({
  fileId: {
    type: [Number, String],
    default: null,
  },
  fileName: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  uploadText: {
    type: String,
    default: '点击上传简历',
  },
  hintText: {
    type: String,
    default: '仅支持 PDF，大小不超过 10 MB',
  },
});

const emit = defineEmits(['uploaded', 'uploading']);

const fileInput = ref(null);
const uploading = ref(false);
const uploadingName = ref('');
const errorMessage = ref('');

const displayText = computed(() => {
  if (uploading.value) {
    return `正在上传：${uploadingName.value}`;
  }
  if (props.fileId) {
    return `已上传：${props.fileName || '简历.pdf'}（点击替换）`;
  }
  return props.uploadText;
});

function openPicker() {
  if (props.disabled || uploading.value) {
    return;
  }
  fileInput.value?.click();
}

function isPdf(file) {
  return file.type === 'application/pdf' || /\.pdf$/i.test(file.name);
}

async function handleFileChange(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) {
    return;
  }

  errorMessage.value = '';

  if (!isPdf(file)) {
    errorMessage.value = '请上传 PDF 格式的简历';
    return;
  }

  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = '简历大小不能超过 10 MB';
    return;
  }

  uploading.value = true;
  uploadingName.value = file.name;
  emit('uploading', true);

  try {
    const data = await uploadResume(file);
    errorMessage.value = '';
    emit('uploaded', data);
  } catch (error) {
    const message = error?.message;
    errorMessage.value =
      typeof message === 'string' && /[一-龥]/.test(message) ? message : '简历上传失败，请重试';
  } finally {
    uploading.value = false;
    uploadingName.value = '';
    emit('uploading', false);
  }
}
</script>

<style scoped>
.file-input {
  display: none;
}

.upload {
  width: 100%;
  padding: 20px;
  border: 1px dashed var(--line2);
  border-radius: 10px;
  background: rgba(250, 250, 252, 0.9);
  color: var(--text3);
  text-align: center;
  cursor: pointer;
  transition: 0.15s;
}

.upload:hover:not(:disabled) {
  border-color: var(--cyan);
  color: var(--text2);
}

.upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload.done {
  border-style: solid;
  border-color: rgba(52, 211, 153, 0.4);
  background: rgba(52, 211, 153, 0.05);
  color: var(--green);
}

.upload.uploading {
  border-color: var(--line2);
  color: var(--cyan);
}

.upload.err {
  border-color: rgba(248, 113, 113, 0.5);
}

.upload svg {
  display: block;
  margin: 0 auto 8px;
}

.upload span {
  display: block;
  font-size: 13.5px;
}

.upload small {
  display: block;
  margin-top: 5px;
  font-size: 11.5px;
  color: var(--text3);
}

.error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--red);
}
</style>

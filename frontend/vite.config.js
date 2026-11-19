import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端用自己的端口，不要和后端 8080 冲突
    host: '0.0.0.0',
    allowedHosts: true, // 允许 trycloudflare.com 等任意 Host
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 指向真正的后端
        changeOrigin: true,
        // 如果后端接口本身不带 /api 前缀，取消下面这行注释
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});

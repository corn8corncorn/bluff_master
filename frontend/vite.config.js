import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  optimizeDeps: {
    include: ['sockjs-client', 'stompjs']
  },
  server: {
    port: 3000,
    host: '0.0.0.0', // 允許外部訪問
    allowedHosts: [
      'localhost',
      '.trycloudflare.com', // 允許所有 Cloudflare Tunnel 域名
      '.local'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})


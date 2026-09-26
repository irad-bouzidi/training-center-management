import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Config files don't see frontend/.env on their own - loadEnv merges it
  // with the real environment (which wins), so VITE_PROXY_TARGET can come
  // from either.
  const env = loadEnv(mode, import.meta.dirname, '')

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(import.meta.dirname, './src'),
      },
    },
    server: {
      // Listen on every interface, not just loopback - inside the dev
      // container (docker-compose.override.yml.example) a loopback-only
      // server is unreachable through the published port, and a phone on
      // the LAN needs it too for the QR check-in demo.
      host: true,
      // The dev-mode twin of nginx.conf's /api proxy: with
      // VITE_API_BASE_URL=/api/v1 the browser only ever talks to the Vite
      // origin. VITE_PROXY_TARGET is the backend as seen from wherever Vite
      // runs - http://backend:8080 inside Compose, localhost on the host.
      // changeOrigin stays false (the default) so the backend sees the
      // browser's own Host header and treats the call as same-origin, with
      // no CORS check involved.
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
        },
      },
    },
  }
})

import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Same-origin dev — the frontend hits its own /api/* which Vite forwards
      // to the backend. Result: no CORS to configure on the backend for dev.
      proxy: {
        '/api':      { target: backend, changeOrigin: true },
        '/actuator': { target: backend, changeOrigin: true },
        '/v3':       { target: backend, changeOrigin: true },
        '/swagger-ui.html': { target: backend, changeOrigin: true },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: true,
    },
  };
});

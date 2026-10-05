import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Kept separate from vite.config.ts so the dev-server proxy config stays uncluttered.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    restoreMocks: true,
    poolOptions: {
      forks: {
        // Node 25 ships its own experimental global `localStorage`, which shadows
        // jsdom's browser implementation (and is unusable without --localstorage-file).
        // Turn Node's off so tests see the jsdom one. Older Node versions (e.g. 20,
        // which CI uses) don't know the flag and refuse to start, so only pass it
        // when the running Node supports it.
        execArgv: process.allowedNodeEnvironmentFlags.has('--experimental-webstorage')
          ? ['--no-experimental-webstorage']
          : [],
      },
    },
  },
});

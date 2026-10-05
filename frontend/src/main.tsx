import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './lib/queryClient';
import { AuthProvider } from './auth/AuthProvider';
import App from './App';
import './styles/index.css';

// The GitHub Pages demo is served from /itams/, the normal build from /.
const basename = import.meta.env.BASE_URL.replace(/\/$/, '') || undefined;

async function start() {
  let badge: React.ReactNode = null;
  // VITE_DEMO is only set for the GitHub Pages build. Vite drops this branch
  // (and the whole demo folder) from the normal production bundle.
  if (import.meta.env.VITE_DEMO === 'true') {
    const { installDemoBackend } = await import('./demo/install');
    const { DemoBadge } = await import('./demo/DemoBadge');
    installDemoBackend();
    badge = <DemoBadge />;
  }

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter basename={basename}>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
      {badge}
    </React.StrictMode>
  );
}

void start();

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { SessionEditor } from './SessionEditor';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionEditor />
  </StrictMode>,
);

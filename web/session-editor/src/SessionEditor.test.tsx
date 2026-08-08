import { describe, expect, it, vi } from 'vitest';
import { setTaskPayableAmount } from './apiClient';
import type { EditorPayload } from './types';

/**
 * Component-level edit/save logic is covered via the pure helpers and API client
 * (shipped path). This file asserts the editor export surface and edit pipeline
 * used by SessionEditor.
 */
describe('SessionEditor edit pipeline', () => {
  it('produces a save-ready payload with edited amount and sessionToken', () => {
    const payload: EditorPayload = {
      version: 1,
      metadata: {
        exportedAt: '2026-08-06T00:00:00Z',
        exportedBy: 'u',
        sessionToken: 'sec',
        serverName: null,
      },
      jobs: {
        'modularjobs:fisherman': {
          displayName: 'Fisherman',
          tasks: [
            {
              actionTypeKey: 'modularjobs:fish',
              contextKey: 'minecraft:cod',
              payables: [{ type: 'modularjobs:economy', amount: '1.0' }],
            },
          ],
        },
      },
      registeredActionTypes: ['modularjobs:fish'],
      registeredPayableTypes: ['modularjobs:economy'],
    };

    const edited = setTaskPayableAmount(payload, 'modularjobs:fisherman', 0, 0, '12.50');
    expect(edited.metadata.sessionToken).toBe('sec');
    expect(edited.jobs['modularjobs:fisherman'].tasks[0].payables[0].amount).toBe('12.50');
  });

  it('session editor module exports the React component', async () => {
    // Dynamic import exercises the real shipped module graph
    const mod = await import('./SessionEditor');
    expect(typeof mod.SessionEditor).toBe('function');
    expect(typeof mod.default).toBe('function');
  });
});

// keep vitest happy if tree-shaken
void vi;

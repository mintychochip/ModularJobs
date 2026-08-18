import { describe, expect, it } from 'vitest'
import { normalizeRelease, toPlainNotes } from './releases'

describe('normalizeRelease', () => {
  it('chooses the Paper jar and preserves release assets', () => {
    const release = normalizeRelease({
      tag_name: 'v3.4.0',
      name: 'ModularJobs 3.4.0',
      html_url: 'https://github.com/aincraft-org/modularjobs/releases/tag/v3.4.0',
      published_at: '2026-08-18T10:00:00Z',
      body: '## Changes\n* Added a paper build',
      assets: [
        {
          name: 'modularjobs-api-3.4.0.jar',
          browser_download_url: 'https://example.test/api.jar',
          size: 10,
          download_count: 2,
        },
        {
          name: 'modularjobs-paper-3.4.0.jar',
          browser_download_url: 'https://example.test/paper.jar',
          size: 20,
          download_count: 4,
        },
      ],
    })

    expect(release?.version).toBe('3.4.0')
    expect(release?.primary?.name).toBe('modularjobs-paper-3.4.0.jar')
    expect(release?.assets).toHaveLength(2)
    expect(release?.notes).toBe('Changes\n• Added a paper build')
  })

  it('falls back to the first jar when no Paper jar exists', () => {
    const release = normalizeRelease({
      tag_name: '1.0.0',
      assets: [
        {
          name: 'modularjobs.jar',
          browser_download_url: 'https://example.test/modularjobs.jar',
        },
      ],
    })

    expect(release?.primary?.name).toBe('modularjobs.jar')
  })

  it('rejects malformed release payloads', () => {
    expect(normalizeRelease(null)).toBeNull()
    expect(normalizeRelease({ assets: [] })).toBeNull()
  })
})

describe('toPlainNotes', () => {
  it('removes links from generated GitHub notes', () => {
    expect(
      toPlainNotes(
        '## What changed\n* Build by @mintychochip in https://github.com/example/pull/1',
      ),
    ).toBe('What changed\n• Build by @mintychochip')
  })
})

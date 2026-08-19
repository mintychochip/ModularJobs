import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { DISCORD_URL } from '../lib/site'
import { GITHUB_URL, LATEST_RELEASE_API } from '../lib/releases'
import { HomePage } from './HomePage'

const STUB_RELEASE = {
  tag_name: 'v7.3.1-test',
  name: 'ModularJobs 7.3.1-test',
  html_url: 'https://github.com/aincraft-org/modularjobs/releases/tag/v7.3.1-test',
  published_at: '2026-08-18T10:00:00Z',
  body: '## Notes\n* Paper jar for the homepage test',
  assets: [
    {
      name: 'modularjobs-paper-7.3.1-test.jar',
      browser_download_url: 'https://example.test/modularjobs-paper-7.3.1-test.jar',
      size: 2048,
      download_count: 3,
    },
  ],
}

function mount(ui: ReactNode) {
  const container = document.createElement('div')
  document.body.appendChild(container)
  let root!: Root
  act(() => {
    root = createRoot(container)
    root.render(ui)
  })
  return {
    container,
    unmount() {
      act(() => {
        root.unmount()
      })
      container.remove()
    },
  }
}

async function flush() {
  await act(async () => {
    await Promise.resolve()
    await Promise.resolve()
  })
}

describe('HomePage', () => {
  beforeAll(() => {
    ;(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true
  })

  afterAll(() => {
    ;(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = false
  })

  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme')
    window.localStorage.clear()
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        addEventListener() {},
        removeEventListener() {},
        addListener() {},
        removeListener() {},
        dispatchEvent() {
          return false
        },
      }),
    })
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        if (url !== LATEST_RELEASE_API) {
          throw new Error(`unexpected fetch: ${url}`)
        }
        return {
          ok: true,
          json: async () => STUB_RELEASE,
        }
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    document.body.replaceChildren()
    document.documentElement.removeAttribute('data-theme')
    window.localStorage.clear()
  })

  it('renders the marketing sections and primary nav', async () => {
    const { container, unmount } = mount(<HomePage />)
    await flush()

    const text = container.textContent ?? ''
    expect(text).toContain('ModularJobs')
    expect(text).toContain('Download')
    expect(text).toContain('Why ModularJobs?')
    expect(text).toContain('Built for progression')
    expect(text).toContain('Quick Start')
    expect(container.querySelector('footer')?.textContent).toMatch(/ModularJobs/)

    expect(container.querySelector('a[href="#download"]')).not.toBeNull()
    expect(container.querySelector('a[href="/docs/"]')).not.toBeNull()
    expect(container.querySelector(`a[href="${DISCORD_URL}"]`)).not.toBeNull()
    expect(container.querySelector(`a[href="${GITHUB_URL}"]`)).not.toBeNull()

    unmount()
  })

  it('toggles data-theme and persists theme-preference', async () => {
    document.documentElement.dataset.theme = 'light'
    const { container, unmount } = mount(<HomePage />)
    await flush()

    const toggle = container.querySelector<HTMLButtonElement>('[data-theme-toggle]')
    expect(toggle).not.toBeNull()

    act(() => {
      toggle!.click()
    })

    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(window.localStorage.getItem('theme-preference')).toBe('dark')

    act(() => {
      toggle!.click()
    })

    expect(document.documentElement.dataset.theme).toBe('light')
    expect(window.localStorage.getItem('theme-preference')).toBe('light')

    unmount()
  })

  it('renders the stubbed GitHub latest release on the download card', async () => {
    const { container, unmount } = mount(<HomePage />)
    await flush()

    const card = container.querySelector('[data-release-card]')
    expect(card).not.toBeNull()
    expect(card?.textContent).toContain(STUB_RELEASE.tag_name)
    expect(card?.textContent).toContain(STUB_RELEASE.assets[0].name)
    expect(card?.textContent).toContain(STUB_RELEASE.name)
    expect(container.querySelector('[data-release-version]')?.textContent).toBe(
      STUB_RELEASE.tag_name.replace(/^v/i, ''),
    )
    expect(fetch).toHaveBeenCalledWith(
      LATEST_RELEASE_API,
      expect.objectContaining({
        headers: expect.objectContaining({
          Accept: 'application/vnd.github+json',
        }),
      }),
    )

    unmount()
  })
})

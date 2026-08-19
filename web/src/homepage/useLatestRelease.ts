import { useCallback, useEffect, useRef, useState } from 'react'
import {
  fetchLatestRelease,
  formatRelativeTime,
  REFRESH_INTERVAL_MS,
  type Release,
} from '../lib/releases'

export interface ReleaseStatus {
  state: 'loading' | 'idle' | 'error'
  text: string
}

export function useLatestRelease(initialRelease: Release | null = null) {
  const [release, setRelease] = useState<Release | null>(initialRelease)
  const [status, setStatus] = useState<ReleaseStatus>({
    state: 'idle',
    text: 'Synced with GitHub releases.',
  })
  const lastSynced = useRef(Date.now())
  const inFlight = useRef(false)

  const showSynced = useCallback(() => {
    setStatus({
      state: 'idle',
      text: `Synced with GitHub releases · updated ${formatRelativeTime(lastSynced.current)}`,
    })
  }, [])

  const refresh = useCallback(async () => {
    if (inFlight.current) return
    inFlight.current = true
    setStatus({
      state: 'loading',
      text: 'Checking GitHub for the latest build…',
    })

    const next = await fetchLatestRelease({ cache: 'no-store' })
    inFlight.current = false

    if (!next) {
      setStatus({
        state: 'error',
        text: 'Could not reach GitHub — showing the last known build.',
      })
      return
    }

    setRelease(next)
    lastSynced.current = Date.now()
    showSynced()
  }, [showSynced])

  useEffect(() => {
    void refresh()

    const refreshIfStale = () => {
      if (document.visibilityState !== 'visible') return
      if (Date.now() - lastSynced.current < REFRESH_INTERVAL_MS) return
      void refresh()
    }

    document.addEventListener('visibilitychange', refreshIfStale)
    window.addEventListener('focus', refreshIfStale)
    const staleTimer = window.setInterval(refreshIfStale, 60 * 1000)
    const tickTimer = window.setInterval(() => {
      if (!lastSynced.current) return
      setStatus((current) =>
        current.state === 'idle'
          ? {
              state: 'idle',
              text: `Synced with GitHub releases · updated ${formatRelativeTime(lastSynced.current)}`,
            }
          : current,
      )
    }, 30 * 1000)

    return () => {
      document.removeEventListener('visibilitychange', refreshIfStale)
      window.removeEventListener('focus', refreshIfStale)
      window.clearInterval(staleTimer)
      window.clearInterval(tickTimer)
    }
  }, [refresh])

  return { release, status, refresh }
}

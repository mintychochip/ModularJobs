export const THEME_STORAGE_KEY = 'theme-preference'

export type Appearance = 'light' | 'dark'
export type ThemePreference = Appearance | 'system'

const THEME_COLOR: Record<Appearance, string> = {
  light: '#f7f7f5',
  dark: '#121212',
}

export const THEME_LABELS: Record<Appearance, string> = {
  light: 'Switch to dark theme',
  dark: 'Switch to light theme',
}

export function readDocumentTheme(): ThemePreference {
  const stored = document.documentElement.dataset.theme
  if (stored === 'light' || stored === 'dark') return stored
  return 'system'
}

export function effectiveTheme(): Appearance {
  const stored = readDocumentTheme()
  if (stored === 'light' || stored === 'dark') return stored
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(theme: ThemePreference): Appearance {
  document.documentElement.dataset.theme = theme
  const appearance = effectiveTheme()
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute('content', THEME_COLOR[appearance])
  return appearance
}

export function persistAndApplyTheme(theme: Appearance): Appearance {
  window.localStorage.setItem(THEME_STORAGE_KEY, theme)
  return applyTheme(theme)
}

export function toggleStoredTheme(): Appearance {
  const next: Appearance = effectiveTheme() === 'light' ? 'dark' : 'light'
  return persistAndApplyTheme(next)
}

import { useEffect, useState } from 'react'
import {
  formatAssetDetail,
  formatDate,
  GITHUB_URL,
  LATEST_RELEASE_URL,
  RELEASES_URL,
  type Release,
} from '../lib/releases'
import { DISCORD_URL, DOCS_URL } from '../lib/site'
import {
  applyTheme,
  readDocumentTheme,
  THEME_LABELS,
  toggleStoredTheme,
} from './theme'
import { useLatestRelease, type ReleaseStatus } from './useLatestRelease'

export interface HomePageProps {
  initialRelease?: Release | null
}

export function HomePage({ initialRelease = null }: HomePageProps) {
  const { release, status, refresh } = useLatestRelease(initialRelease)

  return (
    <>
      <NavBar />
      <main className="main">
        <Hero version={release?.version} />
        <Download release={release} status={status} onRefresh={refresh} />
        <Features />
        <Spotlight />
        <QuickStart />
        <Footer />
      </main>
    </>
  )
}

function NavBar() {
  return (
    <header className="nav">
      <a className="nav-brand" href="/">
        <img className="nav-mark" src="/houston.webp" alt="" width={28} height={28} />
        <span>ModularJobs</span>
      </a>
      <div className="nav-actions">
        <ThemeToggle />
        <nav className="nav-links" aria-label="Primary">
          <a className="nav-link" href="#download">
            Download
          </a>
          <a className="nav-link" href={DOCS_URL}>
            Docs
          </a>
          <a className="nav-link" href={DISCORD_URL} target="_blank" rel="noopener">
            Discord
          </a>
          <a className="nav-link" href={GITHUB_URL} target="_blank" rel="noopener">
            GitHub
          </a>
        </nav>
      </div>
    </header>
  )
}

function ThemeToggle() {
  const [label, setLabel] = useState(THEME_LABELS.light)

  useEffect(() => {
    setLabel(THEME_LABELS[applyTheme(readDocumentTheme())])

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => {
      if (readDocumentTheme() === 'system') {
        setLabel(THEME_LABELS[applyTheme('system')])
      }
    }
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  return (
    <button
      type="button"
      className="icon-button theme-toggle"
      data-theme-toggle=""
      aria-label={label}
      title={label}
      onClick={() => {
        setLabel(THEME_LABELS[toggleStoredTheme()])
      }}
    >
      <svg className="icon-sun" viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
        <circle cx="10" cy="10" r="4" fill="currentColor"></circle>
        <g stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
          <line x1="10" y1="2" x2="10" y2="4"></line>
          <line x1="10" y1="16" x2="10" y2="18"></line>
          <line x1="2" y1="10" x2="4" y2="10"></line>
          <line x1="16" y1="10" x2="18" y2="10"></line>
          <line x1="4.2" y1="4.2" x2="5.6" y2="5.6"></line>
          <line x1="14.4" y1="14.4" x2="15.8" y2="15.8"></line>
          <line x1="14.4" y1="5.6" x2="15.8" y2="4.2"></line>
          <line x1="4.2" y1="15.8" x2="5.6" y2="14.4"></line>
        </g>
      </svg>
      <svg className="icon-moon" viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
        <path
          d="M11.5 2.2a7.5 7.5 0 1 0 6.3 11.3A6.5 6.5 0 1 1 11.5 2.2Z"
          fill="currentColor"
        ></path>
      </svg>
    </button>
  )
}

function Hero({ version }: { version?: string }) {
  return (
    <section className="hero">
      <p className="hero-role">
        PaperMC plugin
        <span data-release-version-line="" hidden={!version}>
          {' '}
          · v<span data-release-version="">{version}</span>
        </span>
      </p>
      <h1 className="hero-name">ModularJobs</h1>
      <p className="hero-tagline">
        An extensible job progression plugin for Minecraft servers — configure jobs, tasks,
        payables, boosts, and upgrade trees, all from a secure web editor.
      </p>
      <div className="hero-actions">
        <a className="button button-primary" href="#download">
          Download latest build
        </a>
        <a className="button" href={DOCS_URL}>
          Get started
        </a>
        <a className="button" href={GITHUB_URL} target="_blank" rel="noopener">
          GitHub
        </a>
      </div>
    </section>
  )
}

function Download({
  release,
  status,
  onRefresh,
}: {
  release: Release | null
  status: ReleaseStatus
  onRefresh: () => void
}) {
  const primaryUrl = release?.primary?.url ?? LATEST_RELEASE_URL
  const primaryLabel = release ? `Download ${release.tag}` : 'Download latest build'

  return (
    <section className="section" id="download">
      <h2 className="section-title">Download</h2>
      <p className="section-intro">
        Builds are published straight from GitHub releases, so this page tracks the newest jar
        without waiting on a redeploy.
      </p>

      <div className="release-card" data-release-card="">
        <div className="release-banner">
          <div className="release-heading">
            <h3 className="release-title" data-release-title="">
              {release ? release.name : 'Latest build'}
            </h3>
            <p className="release-meta" data-release-meta="">
              {release ? (
                <>
                  <span className="release-tag">{release.tag}</span>
                  {release.publishedAt && ` · published ${formatDate(release.publishedAt)}`}
                </>
              ) : (
                'Checking GitHub for the latest release…'
              )}
            </p>
          </div>
          <div className="release-actions">
            <a className="button button-primary" href={primaryUrl} data-release-primary="">
              {primaryLabel}
            </a>
            <a
              className="button"
              href={release?.url ?? RELEASES_URL}
              target="_blank"
              rel="noopener"
              data-release-page=""
            >
              All releases
            </a>
          </div>
        </div>

        <div className="release-body">
          <h4 className="release-assets-title">Assets</h4>
          <ul className="asset-list" data-release-assets="">
            {release ? (
              release.assets.map((asset) => (
                <li className="asset-item" key={asset.url}>
                  <a className="asset-link" href={asset.url}>
                    <span className="asset-name">{asset.name}</span>
                    <span className="asset-detail">{formatAssetDetail(asset)}</span>
                  </a>
                </li>
              ))
            ) : (
              <li className="asset-item">
                <a className="asset-link" href={RELEASES_URL} target="_blank" rel="noopener">
                  <span className="asset-name">Browse assets on GitHub</span>
                </a>
              </li>
            )}
          </ul>

          <div className="release-notes" data-release-notes="" hidden={!release?.notes}>
            <h4 className="release-notes-title">Release notes</h4>
            <p className="release-notes-body" data-release-notes-body="">
              {release?.notes}
            </p>
          </div>

          <p className="release-status" data-release-status="" data-state={status.state}>
            <span data-release-status-text="">{status.text}</span>
            <button type="button" className="release-refresh" data-release-retry="" onClick={onRefresh}>
              Refresh
            </button>
          </p>
        </div>
      </div>
    </section>
  )
}

const FEATURES = [
  {
    title: 'Jobs & Tasks',
    description:
      'Model jobs with configurable tasks, experience curves, and per-action progression across 40+ action types.',
  },
  {
    title: 'Payables',
    description:
      'Attach optional economy rewards and custom payables to levels, tasks, and progression milestones.',
  },
  {
    title: 'Boosts',
    description:
      'Configure timed, item-based, and context-aware boosts to accelerate earning and progression.',
  },
  {
    title: 'Upgrade Trees',
    description: 'Apply profession services and upgrade trees that grow alongside your players.',
  },
  {
    title: 'MySQL 8',
    description:
      'Persist progression and editor sessions with operator-managed, schema-owned MySQL 8.',
  },
  {
    title: 'Secure Editor',
    description:
      'Manage jobs from a secure web editor backed by a dedicated REST API, enabled only when configured.',
  },
]

function Features() {
  return (
    <section className="section" id="features">
      <h2 className="section-title">Why ModularJobs?</h2>
      <p className="section-intro">
        Everything you need to run a configurable job progression system.
      </p>
      <ul className="feature-list">
        {FEATURES.map(({ title, description }) => (
          <li className="feature-item" key={title}>
            <span className="feature-name">{title}</span>
            <p className="feature-description">{description}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}

function Spotlight() {
  return (
    <section className="section feature-spotlight" aria-labelledby="feature-spotlight-title">
      <div className="feature-spotlight-copy">
        <h2 className="section-title" id="feature-spotlight-title">
          Built for progression
        </h2>
        <p className="section-intro">
          Give players a clear path from first job to long-term mastery, without hard-coding your
          server’s design.
        </p>
      </div>
      <ul className="feature-spotlight-list">
        <li>
          <strong>Define the work</strong>
          <span>Combine actions, tasks, and experience curves into jobs that fit your server.</span>
        </li>
        <li>
          <strong>Reward momentum</strong>
          <span>Use payables, boosts, and level-up commands to make every milestone matter.</span>
        </li>
        <li>
          <strong>Keep control</strong>
          <span>Manage progression in MySQL 8 and tune jobs from the secure web editor.</span>
        </li>
      </ul>
    </section>
  )
}

const STEPS = [
  {
    title: 'Apply the MySQL 8 schema',
    description:
      'Point a connect-only MySQL 8 database at ModularJobs and apply the schema out of band. The game and API processes never create tables.',
    code: './scripts/apply-mysql-schema.sh',
  },
  {
    title: 'Install the Paper jar',
    description:
      'Drop the plugin jar into your Paper server’s plugins folder and restart once. Configure database.yml to your schema.',
    code: 'plugins/modularjobs-paper.jar',
  },
  {
    title: 'Run the editor',
    description:
      'Start the secure web editor and manage jobs, tasks, payables, boosts, and upgrade trees from your browser.',
    code: '/jobs editor',
  },
]

function QuickStart() {
  return (
    <section className="section" id="quick-start">
      <h2 className="section-title">Quick Start</h2>
      <p className="section-intro">Up and running in three steps.</p>
      <ol className="step-list">
        {STEPS.map(({ title, description, code }) => (
          <li className="step-item" key={title}>
            <div>
              <h3 className="step-title">{title}</h3>
              <p className="step-description">{description}</p>
              <pre className="step-code">
                <code>{code}</code>
              </pre>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}

function Footer() {
  const year = new Date().getFullYear()
  return (
    <footer className="footer">
      <span>
        © {year} Aincraft — ModularJobs
      </span>
      <div className="footer-links">
        <a className="footer-link" href={DOCS_URL}>
          Docs
        </a>
        <a className="footer-link" href={DISCORD_URL} target="_blank" rel="noopener">
          Discord
        </a>
        <a className="footer-link" href={GITHUB_URL} target="_blank" rel="noopener">
          GitHub
        </a>
      </div>
    </footer>
  )
}

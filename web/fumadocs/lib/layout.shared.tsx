import type { BaseLayoutProps } from 'fumadocs-ui/layouts/shared';
import { appName, gitConfig } from './shared';

export function baseOptions(): BaseLayoutProps {
  return {
    nav: {
      title: (
        <div className="flex items-center gap-2">
          <span className="font-bold tracking-tight">{appName}</span>
          <span className="hidden rounded-md bg-fd-accent px-1.5 py-0.5 text-xs text-fd-accent-foreground sm:inline">
            docs
          </span>
        </div>
      ),
    },
    links: [
      {
        url: '/',
        text: 'Docs',
        active: 'nested-url',
      },
    ],
    githubUrl: `https://github.com/${gitConfig.user}/${gitConfig.repo}`,
  };
}

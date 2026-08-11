// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';

import vue from '@astrojs/vue';

import tailwindcss from '@tailwindcss/vite';

// https://astro.build/config
export default defineConfig({

  integrations: [
    starlight({
      title: 'ModularJobs',
      social: [
        {icon: 'github', label: 'GitHub', href: 'https://github.com/aincraft-org/modularjobs'},
      ],
      sidebar: [
        {
          label: 'Overview',
          items: [{label: 'ModularJobs', slug: 'wiki'}],
        },
        {
          label: 'Operator',
          items: [
            {label: 'Configuration', slug: 'wiki/reference/configuration'},
            {label: 'Operations', slug: 'wiki/guides/operations'},
          ],
        },
        {
          label: 'Features',
          autogenerate: {directory: 'wiki/features'},
        },
      ],
    }),
    vue(),
  ],

  vite: {
    plugins: [tailwindcss()],
  },
});
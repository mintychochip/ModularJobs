// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';

import vue from '@astrojs/vue';

import tailwindcss from '@tailwindcss/vite';

// https://astro.build/config
export default defineConfig({

  integrations: [starlight({
      title: 'Modular Jobs',
      social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/mintychochip/modularjobs' }],
    sidebar: [
          {
              label: 'Guides',
              items: [
                  // Each item here is one entry in the navigation menu.
                  { label: 'Example Guide', slug: 'wiki/guides/example' },
              ],
          },
          {
              label: 'Reference',
              autogenerate: { directory: 'reference' },
          },
      ],
      }), vue()],

  vite: {
    plugins: [tailwindcss()],
  },
});
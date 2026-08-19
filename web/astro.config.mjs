// @ts-check
import {defineConfig} from 'astro/config';
import vue from '@astrojs/vue';
import react from '@astrojs/react';
import tailwindcss from '@tailwindcss/vite';

// https://astro.build/config
export default defineConfig({
  integrations: [
    vue(),
    react(),
  ],

  vite: {
    plugins: [tailwindcss()],
  },
});

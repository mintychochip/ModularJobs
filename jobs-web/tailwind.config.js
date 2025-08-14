// tailwind.config.mjs
import daisyui from 'daisyui'
import { defineConfig } from 'tailwindcss'

export default defineConfig({
  content: [
    './src/**/*.{astro,html,js,ts,vue}', // must include .vue
  ],
  theme: {
    extend: {},
  },
  daisyui: {
    themes: ["light","dark","winter"]
  },
  plugins: [daisyui],
})

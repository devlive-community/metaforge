import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const root = path.resolve(__dirname, 'playground')

// Serves the component playground; the library itself ships raw TS sources
// consumed through the pnpm workspace (see package.json `exports`).
export default defineConfig({
    root,
    plugins: [vue(), tailwindcss()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        fs: {
            allow: [path.resolve(__dirname)],
        },
    },
    build: {
        outDir: path.resolve(__dirname, 'playground-dist'),
        emptyOutDir: true,
    },
})

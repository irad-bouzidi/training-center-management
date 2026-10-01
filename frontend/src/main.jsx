import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider } from 'next-themes'
import './index.css'
import './i18n'
import App from './App.jsx'
import { THEME_STORAGE_KEY } from '@/components/ThemeSwitcher'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* Adds `.dark` to <html> (the class src/index.css's dark variant keys
        on). Defaults to the OS preference until the user picks a theme. */}
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem storageKey={THEME_STORAGE_KEY} disableTransitionOnChange>
      <App />
    </ThemeProvider>
  </StrictMode>,
)

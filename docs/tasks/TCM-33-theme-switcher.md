# TCM-33 — Light / Dark Theme Switcher

**Branch**: `TCM-33-theme-switcher`
**Depends on**: TCM-32 (switcher labels are translated)

## Goal

The user can switch between a light theme, a dark theme, and following the
OS setting. Both themes use a palette that is easy on the eyes.

## Design

- **Library**: `next-themes` (already a dependency; `sonner` reads it).
  `ThemeProvider` in `main.jsx` puts `.dark` on `<html>`, which is the class
  the `dark` variant in `src/index.css` uses. It defaults to `system`, and
  the choice is saved in `localStorage["tcm_theme"]`.
- **No flash**: a small inline script in `index.html` applies the saved theme
  before the bundle loads.
- **Switcher**: `src/components/ThemeSwitcher.jsx` (Light / Dark / System),
  next to the language switcher in the `AppShell` top bar and on the login
  page. Labels are in `common:theme.*`.
- **Palette** (`src/index.css`): neutrals have a slight cool tint toward an
  indigo primary (hue 264) instead of flat grey. Dark mode uses deep
  blue-slate rather than pure black, and cards are one step lighter than the
  page. Every text/background pairing meets WCAG AA (4.5:1) in both themes.
  Re-check contrast whenever you change a token.
- **Status tokens**: `--success` and `--warning` (with `--destructive`) color
  the attendance-rate bars green / amber / red.
- Components use tokens only. The one exception is the white backing behind
  the QR code, which must stay white so the code scans in dark mode.

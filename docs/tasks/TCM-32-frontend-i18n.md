# TCM-32 — Frontend Internationalization (English / French)

**Branch**: `TCM-32-frontend-i18n`
**Depends on**: TCM-4 … TCM-31 (every frontend feature)

## Goal

Every user-visible string in the frontend is available in English and
French, the user can switch language at any time (login page included),
and dates, times and numbers are formatted for the selected language.

## Design

- **Library**: `i18next` + `react-i18next`, with
  `i18next-browser-languagedetector`. Set up in `src/i18n/index.js`,
  imported once from `main.jsx`.
- **Language resolution**: an explicit choice (stored in
  `localStorage["tcm_language"]`) wins, then the browser's preference
  (`fr-FR`, `fr-CA`… → `fr`), then English. `<html lang>` follows the
  active language.
- **Catalogues**: one file per language, `src/locales/en.json` and
  `src/locales/fr.json`, both with the same keys. The top-level keys are
  namespaces: one per feature (`users`, `courses`, …) plus these shared
  ones:
  - `common`: app name, shared actions/states, pagination, and a label
    for every backend enum value (`common:enums.courseStatus.PUBLISHED`,
    `common:enums.paymentStatus.OVERDUE`, …);
  - `layout`: role titles and sidebar navigation;
  - `auth`: the login page;
  - `ui`: text inside the shadcn primitives (e.g. sr-only "Close").
- **Formatting**: `src/lib/format.js` formats with the UI language rather
  than the OS locale. Every feature's `*Display.js` helper uses it.
- **Switcher**: `src/components/LanguageSwitcher.jsx`, shown in the
  `AppShell` top bar and on the login page.

## Backend error messages

The backend's `ApiError.message` is English text and has no machine-readable
code, so the frontend recognizes each message and translates it:

- `apiClient` sends `Accept-Language: en`. Without it, Spring would localize
  bean-validation messages after the browser's language, and the frontend
  could no longer recognize them.
- The response interceptor parses `message` with
  `src/api/serverErrorRules.js` (one rule per backend message, with values
  such as amounts, percentages and statuses captured). It attaches the result
  to the error as `error.serverError`. JSON errors returned to blob requests
  (the certificate download) are read too.
- `apiErrorMessage(error, fallback)` (`src/api/serverErrors.js`) translates
  that result from the `serverErrors` namespace at call time. Amounts and
  percentages are formatted for the language, and enum values are
  translated. Dialogs keep the error object in state and call it at render,
  so a message already on screen follows a language switch. Validation
  errors ("field: message; …") are translated field by field.
- A message no rule knows is shown as-is in English. In French it is
  replaced by the translated fallback of the action that failed.
- `npm run check:server-errors` scans the backend's Java source for every
  message its exceptions can carry and fails if the rules don't recognize
  one or if either language lacks its translation. It also renders every
  message through the real translator in both languages. **When you add or
  change a backend error message, add or adjust its rule and its en/fr
  strings.** Access-denied texts aren't covered, because
  `GlobalExceptionHandler` replaces them with one generic message.

## Conventions

- Components call `useTranslation('<namespace>')`. For another namespace,
  prefix the key (`t('common:actions.save')`).
- Zod schema messages are i18n keys, translated where they render
  (`{t(errors.email.message)}`). A language switch then re-translates any
  error already on screen.
- Mutation hooks call `useTranslation` themselves, so their toasts are
  translated.
- Interpolate with `{{name}}`. Never build a sentence out of translated
  fragments. Plurals use `_one` / `_other`, and French also needs `_many`
  (a copy of `_other`): `Intl.PluralRules('fr')` uses it for 1,000,000 and
  up. Write counts as `{{count, number}}` so the number is formatted for
  the language ("1 284").
- An error kept in component state stores only the backend's message, or
  `null`. The translated fallback is resolved at render, so it follows a
  language switch.
- French typography: `’` for apostrophes, and a narrow no-break space
  (U+202F) before `: ; ? !` and inside `« »`.

## Out of scope

- The content of the generated certificate PDF (backend).
- The browser's built-in validation bubbles on the number inputs of the
  payment and grade dialogs (`min`, `max`, `step`). They appear in the
  browser's language, not the UI's.
- User-entered data such as course titles and descriptions.

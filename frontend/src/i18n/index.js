import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import en from '@/locales/en.json'
import fr from '@/locales/fr.json'

export const SUPPORTED_LANGUAGES = ['en', 'fr']
export const LANGUAGE_STORAGE_KEY = 'tcm_language'

// One catalogue per language: src/locales/<lng>.json. Its top-level keys are
// namespaces - one per feature (users, courses, …) plus `common` for strings
// shared across them (actions, table states, pagination, enum labels) - so
// components still say useTranslation('courses') / t('common:actions.save').
// Bundled eagerly: the whole catalogue is a few KB, not worth lazy-loading.
const resources = { en, fr }

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    supportedLngs: SUPPORTED_LANGUAGES,
    // "fr-FR" / "fr-CA" from the browser resolve to "fr".
    nonExplicitSupportedLngs: true,
    load: 'languageOnly',
    fallbackLng: 'en',
    defaultNS: 'common',
    ns: Object.keys(resources.en),
    detection: {
      // An explicit choice from the language switcher wins; otherwise the
      // browser's preference, and English when that isn't supported.
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: LANGUAGE_STORAGE_KEY,
      caches: ['localStorage'],
    },
    interpolation: {
      // React already escapes rendered values.
      escapeValue: false,
    },
    returnNull: false,
  })

// <html lang> and the tab title follow the UI language.
function syncDocumentLanguage(lng) {
  document.documentElement.lang = lng
  document.title = i18n.t('common:appName')
}
syncDocumentLanguage(i18n.resolvedLanguage ?? 'en')
i18n.on('languageChanged', (lng) => syncDocumentLanguage(i18n.resolvedLanguage ?? lng))

export default i18n

import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import et from "./locales/et.json";

/**
 * Estonian ships in the main bundle (the default and fallback language); Russian and English are
 * separate chunks loaded only for readers who use them, so an Estonian first visit does not
 * download two more full dictionaries (~30-40 KB gzipped).
 */
const LOADERS: Record<string, () => Promise<{ default: Record<string, unknown> }>> = {
  ru: () => import("./locales/ru.json"),
  en: () => import("./locales/en.json"),
};

async function ensureLoaded(lng: string): Promise<void> {
  if (lng === "et" || i18n.hasResourceBundle(lng, "translation")) return;
  const load = LOADERS[lng];
  if (!load) return;
  const mod = await load();
  i18n.addResourceBundle(lng, "translation", mod.default, true, true);
}

/** Switch language, fetching its dictionary first so the UI never flashes untranslated keys. */
export async function setLanguage(lng: string): Promise<void> {
  await ensureLoaded(lng);
  await i18n.changeLanguage(lng);
}

const initDone = i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { et: { translation: et } },
    // Estonian-first: this is an Estonian Parliament site, so a first visit lands in Estonian
    // rather than auto-switching to the browser's language. A saved choice (localStorage) or an
    // explicit ?lng= wins; otherwise we fall back to Estonian. Users switch via the locale picker.
    fallbackLng: "et",
    supportedLngs: ["en", "et", "ru"],
    partialBundledLanguages: true,
    interpolation: { escapeValue: false },
    detection: { order: ["querystring", "localStorage"], caches: ["localStorage"] },
  });

/**
 * Resolves once the detected language's dictionary is in place. main.tsx waits for it before the
 * first render, so a Russian or English reader never sees an Estonian flash (Estonian readers wait
 * for nothing: their dictionary is already bundled).
 */
export const i18nReady: Promise<void> = (async () => {
  await initDone;
  const lng = i18n.language?.split("-")[0] ?? "et";
  try {
    await ensureLoaded(lng);
    if (lng !== "et") await i18n.changeLanguage(lng);
  } catch {
    // Chunk failed to load: stay usable in Estonian rather than render nothing.
    await i18n.changeLanguage("et");
  }
})();

export default i18n;

import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import et from "./locales/et.json";
import ru from "./locales/ru.json";

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { en: { translation: en }, et: { translation: et }, ru: { translation: ru } },
    // Estonian-first: this is an Estonian Parliament site, so a first visit lands in Estonian
    // rather than auto-switching to the browser's language. A saved choice (localStorage) or an
    // explicit ?lng= wins; otherwise we fall back to Estonian. Users switch via the locale picker.
    fallbackLng: "et",
    supportedLngs: ["en", "et", "ru"],
    interpolation: { escapeValue: false },
    detection: { order: ["querystring", "localStorage"], caches: ["localStorage"] },
  });

export default i18n;

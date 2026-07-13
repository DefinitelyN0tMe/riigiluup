import { useTranslation } from "react-i18next";

const LOCALES = [
  { code: "en", label: "EN" },
  { code: "et", label: "ET" },
  { code: "ru", label: "RU" },
];

export default function LocaleSwitcher() {
  const { i18n } = useTranslation();
  const current = i18n.resolvedLanguage ?? "en";
  return (
    <div className="flex gap-1 text-xs" role="group" aria-label="Language">
      {LOCALES.map((l) => (
        <button
          key={l.code}
          onClick={() => i18n.changeLanguage(l.code)}
          aria-pressed={current === l.code}
          className={`px-2 py-1 rounded ${
            current === l.code
              ? "bg-estonia text-white"
              : "text-slate-600 hover:text-estonia"
          }`}
        >
          {l.label}
        </button>
      ))}
    </div>
  );
}

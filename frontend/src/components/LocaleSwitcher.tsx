import { useTranslation } from "react-i18next";

const LOCALES = [
  { code: "et", label: "ET" },
  { code: "en", label: "EN" },
  { code: "ru", label: "RU" },
];

type Variant = "light" | "dark";

export default function LocaleSwitcher({ variant = "dark" }: { variant?: Variant }) {
  const { t, i18n } = useTranslation();
  const current = i18n.resolvedLanguage ?? "et";
  const isLight = variant === "light";
  return (
    <div className="flex gap-0.5 font-mono text-[11px] tracking-[0.08em]" role="group" aria-label={t("a11y.language")}>
      {LOCALES.map((l) => (
        <button
          key={l.code}
          type="button"
          onClick={() => i18n.changeLanguage(l.code)}
          aria-pressed={current === l.code}
          className={`px-2 py-1.5 rounded font-medium transition-colors ${
            current === l.code
              ? isLight ? "text-white font-bold" : "text-blue font-bold"
              : isLight ? "text-white/55 hover:text-white" : "text-muted hover:text-ink"
          }`}
        >
          {l.label}
        </button>
      ))}
    </div>
  );
}

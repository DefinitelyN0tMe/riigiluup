import i18n from "../i18n";

// Map i18next language codes → BCP-47 tags for Intl date formatting.
const BCP47: Record<string, string> = {
  en: "en-GB",
  et: "et-EE",
  ru: "ru-RU",
};

function activeLocale(): string {
  const lng = i18n.resolvedLanguage ?? "en";
  return BCP47[lng] ?? lng;
}

/** Locale-aware date formatting driven by the active i18n language. */
export function formatDate(iso: string | null | undefined, opts?: Intl.DateTimeFormatOptions): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString(activeLocale(), opts);
}

/** Locale-aware date+time formatting driven by the active i18n language. */
export function formatDateTime(iso: string | null | undefined, opts?: Intl.DateTimeFormatOptions): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString(activeLocale(), opts);
}

import i18n from "../i18n";

// Match formatDate.ts: map our i18n codes to BCP-47 locales so the platform
// number formatter uses the right decimal separator (comma in et/ru, point in en).
const BCP47: Record<string, string> = { en: "en-GB", et: "et-EE", ru: "ru-RU" };

function activeLocale(): string {
  const lng = i18n.resolvedLanguage ?? "et";
  return BCP47[lng] ?? "et-EE";
}

/**
 * Format a plain number with a fixed number of fraction digits, using the
 * active UI language's decimal separator.
 * e.g. formatDecimal(73.1) -> "73,1" (et/ru) / "73.1" (en).
 */
export function formatDecimal(value: number, fractionDigits = 1): string {
  return new Intl.NumberFormat(activeLocale(), {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

/**
 * Format a 0..1 rate as a locale-aware percentage string.
 * A literal "%" is appended (no locale percent spacing) to keep the tight
 * "73,1%" form the layout was built around.
 * e.g. formatPercent(0.731) -> "73,1%" (et/ru) / "73.1%" (en).
 */
export function formatPercent(rate: number, fractionDigits = 1): string {
  return `${formatDecimal(rate * 100, fractionDigits)}%`;
}

/**
 * Format an already-scaled percentage value (0..100) as "xx,x%".
 * Use when the caller has already multiplied by 100.
 */
export function formatPercentValue(value: number, fractionDigits = 1): string {
  return `${formatDecimal(value, fractionDigits)}%`;
}

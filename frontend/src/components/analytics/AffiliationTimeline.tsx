import { useTranslation } from "react-i18next";
import type { ExternalAffiliation } from "../../types";

/**
 * Editorially-curated non-Riigikogu affiliation timeline.
 * Every entry MUST have a source. The whole block is prefixed with a "not Riigikogu"
 * disclaimer to preserve the site's "mirror, not interpreter" principle.
 */
export default function AffiliationTimeline({ items }: { items: ExternalAffiliation[] }) {
  const { t, i18n } = useTranslation();
  if (!items.length) return null;

  const fmt = (iso: string | null) => iso ? new Date(iso).toLocaleDateString(i18n.resolvedLanguage, { year: "numeric", month: "short" }) : t("affiliation.present");
  const kindColor = (k: string) => {
    switch (k) {
      case "PARTY": return "#0072CE";
      case "MOVEMENT": return "#003E7E";
      case "FRACTION_ORIGINAL": return "#6BB4F0";
      case "INDEPENDENT": return "#7A7A72";
      default: return "#0A0A0A";
    }
  };

  return (
    <section aria-label={t("affiliation.title")}
             className="rounded-[22px] border border-amber/50 bg-[#FFFBEE] p-5 sm:p-6 md:p-7">
      {/* Disclaimer header */}
      <div className="flex items-start gap-3 pb-4 border-b border-amber/40 mb-5">
        <div className="mt-0.5 w-6 h-6 shrink-0 rounded-full bg-amber text-ink grid place-items-center font-bold text-[13px]">!</div>
        <div>
          <h3 className="font-display font-bold text-[16px] sm:text-[18px] tracking-[-0.015em]">
            {t("affiliation.title")}
          </h3>
          <p className="font-serif italic text-[13px] sm:text-[14px] leading-snug text-ink-2 mt-1">
            {t("affiliation.disclaimer")}
          </p>
        </div>
      </div>

      {/* Timeline */}
      <ol className="relative flex flex-col gap-4 pl-5 sm:pl-6 border-l-2 border-amber/60">
        {items.map((it, i) => (
          <li key={i} className="relative">
            {/* Dot on the line */}
            <span
              aria-hidden
              className="absolute -left-[27px] sm:-left-[31px] top-1.5 w-4 h-4 rounded-full border-4 border-[#FFFBEE]"
              style={{ backgroundColor: kindColor(it.orgKind) }}
            />
            <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 mb-1">
              <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
                {fmt(it.validFrom)} — {fmt(it.validTo)}
              </span>
              <span className="font-mono text-[9px] tracking-[0.16em] uppercase font-bold px-1.5 py-0.5 rounded text-white"
                    style={{ backgroundColor: kindColor(it.orgKind) }}>
                {t(`affiliation.kind.${it.orgKind}`, { defaultValue: it.orgKind })}
              </span>
              {!it.validTo && (
                <span className="inline-flex items-center gap-1.5 font-mono text-[9px] tracking-[0.16em] uppercase font-bold text-live-deep">
                  <span className="w-1.5 h-1.5 rounded-full bg-live shadow-[0_0_6px_theme(colors.live)] animate-pulse-dot" />
                  {t("affiliation.current")}
                </span>
              )}
            </div>
            <div className="font-display font-bold text-[17px] sm:text-[19px] tracking-[-0.015em] leading-tight">
              {it.organization}
            </div>
            {it.role && (
              <div className="text-[13px] sm:text-[14px] text-ink-2 mt-0.5">{it.role}</div>
            )}
            {it.note && (
              <p className="font-serif italic text-[13px] sm:text-[14px] leading-snug text-ink-2 mt-2">
                {it.note}
              </p>
            )}
            <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 font-mono text-[10px] tracking-[0.06em] text-muted">
              <a href={it.sourceUrl} target="_blank" rel="noreferrer noopener"
                 className="text-blue border-b border-blue pb-0.5 hover:opacity-80">
                {t("affiliation.source")}: {it.sourceLabel} ↗
              </a>
              <span>· {t("affiliation.verifiedBy")} <b className="text-ink font-bold">{it.verifiedBy}</b></span>
              <span>· {t("affiliation.verifiedAt")} {fmt(it.verifiedAt)}</span>
            </div>
          </li>
        ))}
      </ol>

      <p className="mt-5 pt-4 border-t border-amber/40 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
        {t("affiliation.footer")}
      </p>
    </section>
  );
}

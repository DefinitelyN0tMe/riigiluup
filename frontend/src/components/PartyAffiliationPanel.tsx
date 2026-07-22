import { useTranslation } from "react-i18next";
import type { PartyMembership } from "../types";

function yearOf(iso: string | null): string | null {
  return iso ? iso.slice(0, 4) : null;
}

/**
 * Combined "political affiliation" panel. Keeps three things DISTINCT and separately labelled:
 * party membership (Wikidata P102, with dates), current parliamentary faction (our data), and
 * the party the MP ran for at the election. "Non-attached" means left the faction — not
 * necessarily the party; the source cannot tell, so the copy never claims otherwise.
 */
export default function PartyAffiliationPanel({
  memberships,
  factionName,
  ranForParty,
}: {
  memberships: PartyMembership[];
  factionName: string | null;
  ranForParty: string | null;
}) {
  const { t } = useTranslation();
  if (!memberships.length && !factionName && !ranForParty) return null;

  const nonAttached = factionName?.toLowerCase().includes("mittekuuluvad") ?? false;

  const periodLabel = (m: PartyMembership): string => {
    const s = yearOf(m.startDate);
    const e = yearOf(m.endDate);
    if (s && e) return `${s}–${e}`;
    if (s) return t("partyAffiliation.since", { year: s });
    if (e) return t("partyAffiliation.until", { year: e });
    return t("partyAffiliation.dateUnknown");
  };

  return (
    <section aria-label={t("partyAffiliation.title")} className="rounded-[22px] border border-rule bg-white p-5 sm:p-6">
      <h3 className="font-display font-bold text-[16px] sm:text-[18px] tracking-[-0.015em] mb-4">
        {t("partyAffiliation.title")}
      </h3>

      <div className="mb-5">
        <div className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-2">
          {t("partyAffiliation.partyMembership")}
        </div>
        {memberships.length ? (
          <ol className="flex flex-col gap-2 list-none p-0">
            {memberships.map((m) => (
              <li key={`${m.partyQid}-${m.startDate ?? "unknown"}`} className="flex items-baseline gap-3">
                <span className="font-mono text-[11px] text-muted tracking-[0.04em] shrink-0 w-28">
                  {periodLabel(m)}
                </span>
                <span className="font-display font-semibold text-[15px] leading-tight">{m.partyLabel}</span>
              </li>
            ))}
          </ol>
        ) : (
          <p className="font-serif italic text-[13px] text-ink-2">{t("partyAffiliation.noPartyData")}</p>
        )}
      </div>

      {factionName && (
        <div className="mb-5">
          <div className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-1">
            {t("partyAffiliation.faction")}
          </div>
          <div className="text-[15px] text-ink">
            {nonAttached ? t("partyAffiliation.nonAttached") : factionName}
          </div>
        </div>
      )}

      {ranForParty && (
        <div className="mb-5">
          <div className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-1">
            {t("partyAffiliation.election")}
          </div>
          <div className="text-[15px] text-ink">{t("partyAffiliation.ranForParty", { party: ranForParty })}</div>
        </div>
      )}

      <p className="pt-4 border-t border-rule font-mono text-[10px] tracking-[0.08em] text-muted leading-relaxed">
        {t("partyAffiliation.source")}
      </p>
    </section>
  );
}

import { useTranslation } from "react-i18next";
import type { PartyFinanceBoard } from "../../api/analytics";

// Fixed categorical colours for income sources (not parties).
const SOURCE_COLORS: Record<string, string> = {
  state: "#0072CE",
  donations: "#1EA98A",
  membership: "#FFB020",
  loans: "#FF4B3E",
  other: "#94A3B8",
};
const SOURCE_ORDER = ["state", "donations", "membership", "loans", "other"] as const;

/**
 * "Money in politics" — each parliamentary party's declared income mix since 2023
 * from the ERJK register. Each bar shows the split by source; the euro total shows size.
 */
export default function PartyFinance({ data }: { data: PartyFinanceBoard }) {
  const { t, i18n } = useTranslation();
  if (!data.parties.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const eur = new Intl.NumberFormat(i18n.resolvedLanguage ?? "en", {
    notation: "compact",
    maximumFractionDigits: 1,
  });
  const eurFull = new Intl.NumberFormat(i18n.resolvedLanguage ?? "en", { maximumFractionDigits: 0 });

  return (
    <div>
      <div className="flex flex-wrap gap-x-5 gap-y-2 mb-5 font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
        {SOURCE_ORDER.map((k) => (
          <span key={k} className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm inline-block" style={{ backgroundColor: SOURCE_COLORS[k] }} />
            {t(`viz.finance.source.${k}`)}
          </span>
        ))}
      </div>

      <ul className="flex flex-col gap-4">
        {data.parties.map((p) => {
          const bucket = Object.fromEntries(p.buckets.map((b) => [b.key, b.amount]));
          return (
            <li key={p.partyName} className="bg-white rounded-[22px] border border-rule p-4 sm:p-5">
              <div className="flex items-baseline justify-between gap-3 mb-2.5">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="w-2 h-2 rounded-full inline-block shrink-0" style={{ backgroundColor: p.colorHex ?? "#0072CE" }} />
                  <span className="font-display font-bold text-[16px] sm:text-[18px] leading-tight tracking-[-0.02em] truncate">
                    {p.partyName}
                  </span>
                </div>
                <span className="font-display font-bold text-[18px] sm:text-[20px] text-ink tabular-nums whitespace-nowrap">
                  €{eur.format(p.total)}
                </span>
              </div>
              <div className="h-3 rounded-full bg-off overflow-hidden flex">
                {SOURCE_ORDER.map((k) => {
                  const amt = bucket[k] ?? 0;
                  if (!amt) return null;
                  const w = (amt / p.total) * 100;
                  return (
                    <div
                      key={k}
                      style={{ width: `${w}%`, backgroundColor: SOURCE_COLORS[k] }}
                      title={`${t(`viz.finance.source.${k}`)}: €${eurFull.format(amt)} (${w.toFixed(0)}%)`}
                    />
                  );
                })}
              </div>
            </li>
          );
        })}
      </ul>

      <p className="mt-4 font-mono text-[10px] tracking-[0.1em] uppercase text-muted">
        {t("viz.finance.footnote", { year: data.sinceYear })}{" · "}
        <a href="https://www.erjk.ee" target="_blank" rel="noreferrer noopener" className="text-blue border-b border-blue">
          ERJK ↗
        </a>
      </p>
    </div>
  );
}

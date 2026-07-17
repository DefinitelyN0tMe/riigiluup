import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ElectionBoard } from "../../api/analytics";

const TOP_N = 15;

/**
 * Elected sitting MPs ranked by personal votes at RK_2023, with a mandate-type
 * breakdown on top. Bars are faction-coloured; every row links to the profile.
 */
export default function ElectionLeaders({ data }: { data: ElectionBoard }) {
  const { t, i18n } = useTranslation();
  if (!data.members.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const ranked = data.members.slice(0, TOP_N); // already votes-desc from the API
  const max = Math.max(...ranked.map((m) => m.personalVotes), 1);

  return (
    <div>
      <div className="flex flex-wrap gap-x-8 gap-y-2 mb-5">
        {data.mandates.map((md) => (
          <div key={md.mandateType} className="flex items-baseline gap-2">
            <span className="font-display font-bold text-[22px] text-ink tabular-nums">{md.count}</span>
            <span className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
              {t(`profile.election.mandate.${md.mandateType}`, md.mandateType)}
            </span>
          </div>
        ))}
      </div>

      <ol className="flex flex-col divide-y divide-rule bg-white rounded-[22px] border border-rule overflow-hidden">
        {ranked.map((m, i) => {
          const width = Math.max(3, (m.personalVotes / max) * 100);
          const color = m.factionColorHex ?? "#0072CE";
          return (
            <li key={m.memberSlug}>
              <Link
                to={`/politicians/${encodeURIComponent(m.memberSlug)}`}
                className="grid grid-cols-[32px_1fr_auto] items-center gap-3 sm:gap-6 px-4 sm:px-6 py-3.5 sm:py-4 hover:bg-off transition-colors"
              >
                <div className="font-serif italic font-light text-[24px] leading-none text-blue">
                  {String(i + 1).padStart(2, "0")}
                </div>
                <div className="min-w-0">
                  <div className="font-display font-bold text-[16px] sm:text-[18px] leading-tight tracking-[-0.02em] truncate">
                    {m.memberName}
                  </div>
                  <div className="flex items-center gap-2 mt-1 font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                    <span className="w-1.5 h-1.5 rounded-full inline-block" style={{ backgroundColor: color }} />
                    {m.factionShortName ?? "—"}
                    <span className="opacity-70">· {t(`profile.election.mandate.${m.mandateType}`, m.mandateType)}</span>
                  </div>
                  <div className="mt-1.5 h-1.5 rounded-full bg-off overflow-hidden max-w-[440px]">
                    <div className="h-full rounded-full" style={{ width: `${width}%`, backgroundColor: color }} />
                  </div>
                </div>
                <div className="font-display font-bold text-[22px] sm:text-[26px] leading-none tracking-[-0.03em] text-ink justify-self-end tabular-nums">
                  {m.personalVotes.toLocaleString(i18n.resolvedLanguage)}
                </div>
              </Link>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

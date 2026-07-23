import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { DisciplineBreakers as Data } from "../../api/analytics";
import { formatDate } from "../../lib/formatDate";

export default function DisciplineBreakers({ data }: { data: Data }) {
  const { t } = useTranslation();
  if (!data.items.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;
  const maxRate = Math.max(...data.items.map((i) => i.deviationRate), 0.001);
  return (
    <ol className="flex flex-col divide-y divide-rule bg-white rounded-[22px] border border-rule overflow-hidden">
      {data.items.map((it, i) => {
        const rateWidth = Math.max(4, (it.deviationRate / maxRate) * 100);
        return (
          <li key={it.memberSlug}>
            <Link to={`/politicians/${encodeURIComponent(it.memberSlug)}`}
              className="grid grid-cols-[36px_1fr] md:grid-cols-[36px_260px_1fr_180px] items-center gap-3 sm:gap-6 px-4 sm:px-6 py-4 sm:py-5 hover:bg-off transition-colors">
              <div className="font-serif italic font-light text-[26px] leading-none text-blue">{String(i + 1).padStart(2, "0")}</div>
              <div className="min-w-0">
                <div className="font-display font-bold text-[17px] sm:text-[19px] leading-tight tracking-[-0.02em] truncate">{it.memberName}</div>
                <div className="flex items-center gap-2 mt-1 font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                  <span className="w-1.5 h-1.5 rounded-full inline-block" style={{ backgroundColor: it.factionColorHex ?? "#0072CE" }} />
                  {it.factionShortName ?? "—"}
                </div>
              </div>
              <div className="hidden md:block min-w-0">
                {it.exampleVoteDescription && (
                  <div className="font-serif italic text-[15px] leading-snug text-ink-2 truncate">
                    «{it.exampleVoteDescription}»
                  </div>
                )}
                {it.exampleVoteDate && (
                  <div className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted mt-1">
                    {t("viz.discipline.example")} {formatDate(it.exampleVoteDate)}
                  </div>
                )}
              </div>
              <div className="col-start-2 md:col-start-auto flex flex-col items-end gap-1 min-w-0">
                <div className="flex items-baseline gap-1.5">
                  <span className="font-display font-bold text-[22px] sm:text-[26px] leading-none tracking-[-0.03em] text-hot">
                    {(it.deviationRate * 100).toFixed(1)}
                  </span>
                  <span className="font-serif italic text-[16px] text-muted">%</span>
                </div>
                <div className="w-full sm:w-[180px] h-1.5 rounded-full bg-off overflow-hidden">
                  <div className="h-full bg-hot" style={{ width: `${rateWidth}%` }} />
                </div>
                <div className="font-mono text-[10px] tracking-[0.06em] text-muted">
                  <b className="text-ink font-bold">{it.deviations}</b> / {it.eligible} {t("viz.discipline.deviations")}
                </div>
              </div>
            </Link>
          </li>
        );
      })}
    </ol>
  );
}

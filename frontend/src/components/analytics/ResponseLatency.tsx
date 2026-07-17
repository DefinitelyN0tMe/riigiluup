import { useTranslation } from "react-i18next";
import type { ResponseLatencyBoard } from "../../api/analytics";

/**
 * Ministers ranked by how they answer parliamentary questions: median days to answer,
 * share answered by the deadline, and questions overdue right now. Deadlines come from
 * the source itself — nothing here is our interpretation.
 */
export default function ResponseLatency({ data }: { data: ResponseLatencyBoard }) {
  const { t, i18n } = useTranslation();
  if (!data.ministers.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const maxMedian = Math.max(...data.ministers.map((m) => m.medianDaysToAnswer ?? 0), 1);

  return (
    <ol className="flex flex-col divide-y divide-rule bg-white rounded-[22px] border border-rule overflow-hidden">
      {data.ministers.map((m, i) => {
        const median = m.medianDaysToAnswer;
        const pctOnTime = m.answered > 0 ? Math.round((m.answeredOnTime / m.answered) * 100) : null;
        // addresseeRole repeats the person's name ("välisminister Margus Tsahkna") — show only the office.
        const office = m.addresseeRole
          ? m.addresseeRole.replace(m.addresseeName, "").trim()
          : null;
        const width = median == null ? 0 : Math.max(3, (median / maxMedian) * 100);
        return (
          <li key={m.addresseeName} className="grid grid-cols-[32px_1fr_auto] items-center gap-3 sm:gap-6 px-4 sm:px-6 py-3.5 sm:py-4">
            <div className="font-serif italic font-light text-[24px] leading-none text-blue">
              {String(i + 1).padStart(2, "0")}
            </div>
            <div className="min-w-0">
              <div className="font-display font-bold text-[16px] sm:text-[18px] leading-tight tracking-[-0.02em] truncate">
                {m.addresseeName}
                {office && <span className="ml-2 font-mono font-normal text-[10px] tracking-[0.1em] uppercase text-muted">{office}</span>}
              </div>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1 font-mono text-[10px] tracking-[0.1em] uppercase text-muted">
                <span>{t("viz.latency.questions", { count: m.total })}</span>
                {pctOnTime != null && (
                  <span className={pctOnTime < 50 ? "text-hot-deep font-bold" : ""}>
                    {t("viz.latency.onTime", { pct: pctOnTime })}
                  </span>
                )}
                {m.overdueNow > 0 && (
                  <span className="text-hot-deep font-bold">
                    {t("viz.latency.overdue", { count: m.overdueNow })}
                  </span>
                )}
              </div>
              <div className="mt-1.5 h-1.5 rounded-full bg-off overflow-hidden max-w-[440px]">
                <div className="h-full rounded-full bg-blue" style={{ width: `${width}%` }} />
              </div>
            </div>
            <div className="justify-self-end text-right">
              <div className="font-display font-bold text-[22px] sm:text-[26px] leading-none tracking-[-0.03em] text-ink tabular-nums">
                {median == null ? "—" : Math.round(median).toLocaleString(i18n.resolvedLanguage)}
              </div>
              <div className="font-mono text-[9px] tracking-[0.14em] uppercase text-muted mt-1">
                {t("viz.latency.medianDays")}
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

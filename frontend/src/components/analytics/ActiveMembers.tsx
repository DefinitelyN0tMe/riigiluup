import { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { MemberActivityBoard, ActivityMetric } from "../../api/analytics";

const METRICS: ActivityMetric[] = ["speeches", "questions", "interpellations", "writtenQuestions"];
const TOP_N = 15;

/**
 * Ranked "most active MPs" board with a metric toggle. Bars are coloured by faction
 * (so party activity reads at a glance); every row links to the MP's profile.
 */
export default function ActiveMembers({ data }: { data: MemberActivityBoard }) {
  const { t, i18n } = useTranslation();
  const [metric, setMetric] = useState<ActivityMetric>("speeches");
  if (!data.items.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const ranked = [...data.items].sort((a, b) => b[metric] - a[metric]).slice(0, TOP_N);
  const max = Math.max(...ranked.map((i) => i[metric]), 1);

  return (
    <div>
      <div role="tablist" aria-label={t("viz.activity.metricLabel")} className="flex flex-wrap gap-2 mb-5">
        {METRICS.map((m) => (
          <button
            key={m}
            type="button"
            role="tab"
            aria-selected={metric === m}
            onClick={() => setMetric(m)}
            title={m === "interpellations" ? t("viz.activity.interpellationsHint") : undefined}
            className={`font-mono text-[11px] tracking-[0.08em] uppercase px-3 py-1.5 rounded-full border transition-colors ${
              metric === m ? "bg-ink text-white border-ink" : "bg-white text-ink border-rule hover:border-blue"
            }`}
          >
            {t(`viz.activity.metric.${m}`)}
          </button>
        ))}
      </div>

      <ol className="flex flex-col divide-y divide-rule bg-white rounded-[22px] border border-rule overflow-hidden">
        {ranked.map((it, i) => {
          const val = it[metric];
          const width = Math.max(3, (val / max) * 100);
          const color = it.factionColorHex ?? "#0072CE";
          return (
            <li key={it.memberSlug}>
              <Link
                to={`/politicians/${encodeURIComponent(it.memberSlug)}`}
                className="grid grid-cols-[32px_1fr_auto] items-center gap-3 sm:gap-6 px-4 sm:px-6 py-3.5 sm:py-4 hover:bg-off transition-colors"
              >
                <div className="font-serif italic font-light text-[24px] leading-none text-blue">
                  {String(i + 1).padStart(2, "0")}
                </div>
                <div className="min-w-0">
                  <div className="font-display font-bold text-[16px] sm:text-[18px] leading-tight tracking-[-0.02em] truncate">
                    {it.memberName}
                  </div>
                  <div className="flex items-center gap-2 mt-1 font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                    <span className="w-1.5 h-1.5 rounded-full inline-block" style={{ backgroundColor: color }} />
                    {it.factionShortName ?? "—"}
                  </div>
                  <div className="mt-1.5 h-1.5 rounded-full bg-off overflow-hidden max-w-[440px]">
                    <div className="h-full rounded-full" style={{ width: `${width}%`, backgroundColor: color }} />
                  </div>
                </div>
                <div className="font-display font-bold text-[22px] sm:text-[26px] leading-none tracking-[-0.03em] text-ink justify-self-end tabular-nums">
                  {val.toLocaleString(i18n.resolvedLanguage)}
                </div>
              </Link>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

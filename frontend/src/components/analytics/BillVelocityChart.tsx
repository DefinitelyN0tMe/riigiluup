import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { BillVelocity } from "../../api/analytics";

const BUCKET_KEYS = ["b_week", "b_month", "b_3months", "b_6months", "b_year", "b_yearPlus"];
// Bucket → (minDaysInclusive, maxDaysInclusive|null)
const BUCKET_RANGE: [number, number | null][] = [
  [0, 7], [8, 30], [31, 90], [91, 180], [181, 365], [366, null],
];

export default function BillVelocityChart({ data }: { data: BillVelocity }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  if (!data.totalAdopted) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;
  const maxCount = Math.max(...data.buckets.map((b) => b.count), 1);
  const barW = 100 / data.buckets.length;

  function openBucket(i: number, label: string, count: number) {
    if (count === 0) return;
    const [min, max] = BUCKET_RANGE[i] ?? [null, null];
    const params = new URLSearchParams();
    params.set("phase", "ADOPTED");
    if (min != null) params.set("minDays", String(min));
    if (max != null) params.set("maxDays", String(max));
    params.set("velocityLabel", label);
    navigate(`/legislation?${params.toString()}`);
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-[1fr_240px] gap-6 sm:gap-10 items-end">
      <div>
        <svg role="group" aria-label={t("viz.a11y.billVelocity", { median: data.medianDays, defaultValue: "Bill velocity histogram, median {{median}} days from initiation to adoption." })}
             viewBox="0 0 100 60" preserveAspectRatio="none" className="w-full h-[240px] sm:h-[280px]">
          {data.buckets.map((b, i) => {
            const h = (b.count / maxCount) * 52;
            const y = 56 - h;
            const isFast = i < 2;
            const isSlow = i >= 4;
            const color = isFast ? "#0072CE" : isSlow ? "#FF4B3E" : "#003E7E";
            const label = t(`viz.velocity.${BUCKET_KEYS[i] ?? "b_week"}`, { defaultValue: b.label });
            const clickable = b.count > 0;
            const barLabel = t("viz.velocity.openBucket", { count: b.count, label });
            return (
              <g key={i}
                 style={{ cursor: clickable ? "pointer" : "default" }}
                 onClick={() => openBucket(i, label, b.count)}
                 tabIndex={clickable ? 0 : undefined}
                 role={clickable ? "link" : undefined}
                 aria-label={clickable ? barLabel : undefined}
                 onKeyDown={clickable ? (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openBucket(i, label, b.count); } } : undefined}>
                <title>{clickable ? barLabel : label}</title>
                {/* Invisible click hit-area for the whole column */}
                <rect x={i * barW} y={0} width={barW} height={60} fill="transparent" />
                <rect x={i * barW + 0.5} y={y} width={barW - 1} height={h} fill={color} rx={0.5} />
                <text x={i * barW + barW / 2} y={y - 1} textAnchor="middle"
                      fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={3.4}
                      fill={color}>
                  {b.count}
                </text>
                <text x={i * barW + barW / 2} y={59}
                      textAnchor="middle" fontFamily="'JetBrains Mono', monospace" fontSize={2.6}
                      fill="#7A7A72" letterSpacing="0.05em">
                  {label}
                </text>
              </g>
            );
          })}
        </svg>
        <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center mt-3">
          ↑ {t("viz.velocity.clickHint")}
        </p>
      </div>
      <ul className="flex flex-col gap-3 font-mono text-[11px]">
        <li className="flex items-baseline justify-between border-b border-rule pb-2">
          <span className="uppercase tracking-[0.16em] text-muted">{t("viz.velocity.median")}</span>
          <span className="font-display font-bold text-[26px] tracking-[-0.03em] text-blue">{data.medianDays}d</span>
        </li>
        <li className="flex items-baseline justify-between border-b border-rule pb-2">
          <span className="uppercase tracking-[0.16em] text-muted">{t("viz.velocity.p90")}</span>
          <span className="font-display font-bold text-[20px] tracking-[-0.03em]">{data.p90Days}d</span>
        </li>
        <li className="flex items-baseline justify-between border-b border-rule pb-2">
          <span className="uppercase tracking-[0.16em] text-muted">{t("viz.velocity.fastest")}</span>
          <span className="font-display font-bold text-[18px] tracking-[-0.03em] text-live">{data.fastestDays}d</span>
        </li>
        <li className="flex items-baseline justify-between">
          <span className="uppercase tracking-[0.16em] text-muted">{t("viz.velocity.slowest")}</span>
          <span className="font-display font-bold text-[18px] tracking-[-0.03em] text-hot">{data.slowestDays}d</span>
        </li>
        <li className="pt-2 text-muted text-[10px] leading-relaxed">
          {t("viz.velocity.footer", { count: data.totalAdopted })} {t("viz.velocity.formula")}
        </li>
      </ul>
    </div>
  );
}

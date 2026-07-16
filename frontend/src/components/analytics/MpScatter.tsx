import { Link, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { MpSimilarity } from "../../api/analytics";

/**
 * 2D scatter: X = coalition (+1) ↔ opposition (-1); Y = party loyalty (+1) ↔ dissenter (-1).
 * Colored by faction. Hover reveals label.
 */
export default function MpScatter({ data }: { data: MpSimilarity }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [hover, setHover] = useState<string | null>(null);
  const points = data.points;
  if (!points.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const size = 640;
  const pad = 44;
  const project = (v: number, axis: "x" | "y") => {
    // v in [-1, 1] → [pad, size - pad]
    const scale = (v + 1) / 2;
    return axis === "x" ? pad + scale * (size - 2 * pad) : size - pad - scale * (size - 2 * pad);
  };

  const factionSummary = useMemo(() => {
    const map = new Map<string, { count: number; color: string }>();
    for (const p of points) {
      const k = p.factionShortName ?? "—";
      const cur = map.get(k) ?? { count: 0, color: p.factionColorHex ?? "#0072CE" };
      cur.count++;
      map.set(k, cur);
    }
    return Array.from(map.entries()).sort((a, b) => b[1].count - a[1].count);
  }, [points]);

  const highlighted = points.find((p) => p.slug === hover);

  return (
    <div className="relative">
      <svg role="group" aria-label={t("viz.a11y.mpScatter", { count: data.points.length, defaultValue: "MP similarity scatterplot with {{count}} MPs, x = coalition-vs-opposition, y = faction loyalty." })}
           viewBox={`0 0 ${size} ${size}`} className="w-full max-w-[720px] mx-auto block">
        {/* Quadrants background */}
        <rect x={pad} y={pad} width={(size - 2 * pad) / 2} height={(size - 2 * pad) / 2} fill="#F4F4F1" />
        <rect x={size / 2} y={size / 2} width={(size - 2 * pad) / 2} height={(size - 2 * pad) / 2} fill="#F4F4F1" />
        {/* Axes */}
        <line x1={pad} y1={size / 2} x2={size - pad} y2={size / 2} stroke="#0A0A0A" strokeWidth={1.2} />
        <line x1={size / 2} y1={pad} x2={size / 2} y2={size - pad} stroke="#0A0A0A" strokeWidth={1.2} />
        {/* Axis labels */}
        <text x={size - pad + 4} y={size / 2 + 4} fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#0072CE">{t("viz.scatter.xRight")}</text>
        <text x={pad - 4} y={size / 2 + 4} textAnchor="end" fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#FF4B3E">{t("viz.scatter.xLeft")}</text>
        <text x={size / 2 + 6} y={pad - 6} fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#0A0A0A">{t("viz.scatter.yUp")}</text>
        <text x={size / 2 + 6} y={size - pad + 14} fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#0A0A0A">{t("viz.scatter.yDown")}</text>
        <text x={pad + 12} y={pad + 18} fontFamily="'Fraunces', serif" fontStyle="italic" fontWeight={300} fontSize={14} fill="#0A0A0A" opacity={0.35}>{t("viz.scatter.qOpLoy")}</text>
        <text x={size - pad - 12} y={pad + 18} textAnchor="end" fontFamily="'Fraunces', serif" fontStyle="italic" fontWeight={300} fontSize={14} fill="#0A0A0A" opacity={0.35}>{t("viz.scatter.qCoLoy")}</text>
        <text x={pad + 12} y={size - pad - 8} fontFamily="'Fraunces', serif" fontStyle="italic" fontWeight={300} fontSize={14} fill="#0A0A0A" opacity={0.35}>{t("viz.scatter.qOpInd")}</text>
        <text x={size - pad - 12} y={size - pad - 8} textAnchor="end" fontFamily="'Fraunces', serif" fontStyle="italic" fontWeight={300} fontSize={14} fill="#0A0A0A" opacity={0.35}>{t("viz.scatter.qCoInd")}</text>

        {/* Points */}
        {points.map((p) => {
          const cx = project(p.x, "x");
          const cy = project(p.y, "y");
          const active = hover === p.slug;
          return (
            <g key={p.slug}
               onMouseEnter={() => setHover(p.slug)} onMouseLeave={() => setHover(null)}
               onFocus={() => setHover(p.slug)} onBlur={() => setHover(null)}
               onClick={() => navigate(`/politicians/${encodeURIComponent(p.slug)}`)}
               tabIndex={0} role="link"
               aria-label={t("viz.scatter.openProfile", { name: p.name })}
               onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); navigate(`/politicians/${encodeURIComponent(p.slug)}`); } }}
               style={{ cursor: "pointer" }}>
              <circle cx={cx} cy={cy} r={active ? 8 : 5}
                      fill={p.factionColorHex ?? "#0072CE"}
                      stroke={active ? "#0A0A0A" : "#FFFFFF"} strokeWidth={active ? 2 : 1}
                      opacity={hover && !active ? 0.35 : 0.9}
                      style={{ transition: "r 0.15s, opacity 0.15s" }} />
            </g>
          );
        })}

        {highlighted && (() => {
          const cx = project(highlighted.x, "x");
          const cy = project(highlighted.y, "y");
          // Flip the tooltip to the left for right-edge points so it doesn't clip off-canvas.
          const flip = cx > size - 240;
          const boxX = flip ? cx - 232 : cx + 12;
          const textX = boxX + 10;
          return (
            <g pointerEvents="none">
              <rect x={boxX} y={cy - 32} width={220} height={54} rx={6} fill="#0A0A0A" opacity={0.92} />
              <text x={textX} y={cy - 15} fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={13} fill="#FFFFFF">
                {highlighted.name}
              </text>
              <text x={textX} y={cy + 3} fontFamily="'JetBrains Mono', monospace" fontSize={10} fill="#6BB4F0" letterSpacing="0.06em">
                {highlighted.factionShortName ?? "—"} · {highlighted.totalComparableVotes} {t("viz.scatter.tooltipVotes")}
              </text>
              <text x={textX} y={cy + 17} fontFamily="'JetBrains Mono', monospace" fontSize={10} fill="#FFFFFF" opacity={0.7} letterSpacing="0.06em">
                {highlighted.deviations} {t("viz.scatter.tooltipDev")}
              </text>
            </g>
          );
        })()}
      </svg>

      {/* Faction legend */}
      <div className="mt-4 flex flex-wrap justify-center gap-3 sm:gap-5 font-mono text-[10px] tracking-[0.14em] uppercase">
        {factionSummary.map(([k, v]) => (
          <span key={k} className="flex items-center gap-2 text-muted">
            <span className="w-3 h-3 rounded-full" style={{ background: v.color }} /> {k} · <b className="text-ink font-bold">{v.count}</b>
          </span>
        ))}
      </div>
      {highlighted && (
        <div className="text-center mt-3">
          <Link to={`/politicians/${encodeURIComponent(highlighted.slug)}`} className="font-mono text-[11px] text-blue tracking-[0.08em] border-b border-blue pb-0.5">
            {t("viz.scatter.openProfile", { name: highlighted.name })}
          </Link>
        </div>
      )}
    </div>
  );
}

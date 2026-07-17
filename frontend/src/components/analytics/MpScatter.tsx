import { Link, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { MpSimilarity } from "../../api/analytics";

/**
 * 2D scatter: X = opposition (-1) ↔ coalition (+1); Y = faction loyalty (+1) ↔ dissenter (-1).
 * Points coloured by faction; hover reveals a label. Axis pole labels read inward so they
 * never clip against the SVG viewport.
 */
export default function MpScatter({ data }: { data: MpSimilarity }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [hover, setHover] = useState<string | null>(null);
  const points = data.points;

  const size = 640;
  const pad = 56;
  const mid = size / 2;
  const project = (v: number, axis: "x" | "y") => {
    // v in [-1, 1] → [pad, size - pad]
    const scale = (v + 1) / 2;
    return axis === "x" ? pad + scale * (size - 2 * pad) : size - pad - scale * (size - 2 * pad);
  };
  const gridlines = [-0.5, 0.5];

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
  const LABEL = "'JetBrains Mono', monospace";

  // After the hooks — an early return above useMemo breaks the rules of hooks
  // (hook order changes when data flips between empty and non-empty).
  if (!points.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  return (
    <div className="relative">
      <svg role="group"
           aria-label={t("viz.a11y.mpScatter", { count: data.points.length, defaultValue: "MP similarity scatterplot with {{count}} MPs, x = coalition-vs-opposition, y = faction loyalty." })}
           viewBox={`0 0 ${size} ${size}`} className="w-full max-w-[720px] mx-auto block">
        {/* Plot surface + frame */}
        <rect x={pad} y={pad} width={size - 2 * pad} height={size - 2 * pad} rx={12}
              fill="#FCFCFA" stroke="#E7E7E1" strokeWidth={1} />

        {/* Faint reference gridlines at ±0.5 */}
        {gridlines.map((g) => (
          <g key={g} stroke="#EDEDE7" strokeWidth={1}>
            <line x1={project(g, "x")} y1={pad} x2={project(g, "x")} y2={size - pad} />
            <line x1={pad} y1={project(g, "y")} x2={size - pad} y2={project(g, "y")} />
          </g>
        ))}

        {/* Center crosshair — the neutral origin */}
        <line x1={pad} y1={mid} x2={size - pad} y2={mid} stroke="#CDCDC6" strokeWidth={1.25} />
        <line x1={mid} y1={pad} x2={mid} y2={size - pad} stroke="#CDCDC6" strokeWidth={1.25} />

        {/* Axis pole labels — placed inside the plot, reading toward each pole (never clip) */}
        <text x={pad + 12} y={mid - 12} textAnchor="start"
              fontFamily={LABEL} fontSize={11} fontWeight={600} letterSpacing="0.09em" fill="#5B5B54">
          {t("viz.scatter.xLeft")}
        </text>
        <text x={size - pad - 12} y={mid - 12} textAnchor="end"
              fontFamily={LABEL} fontSize={11} fontWeight={600} letterSpacing="0.09em" fill="#5B5B54">
          {t("viz.scatter.xRight")}
        </text>
        <text x={mid} y={pad - 13} textAnchor="middle"
              fontFamily={LABEL} fontSize={11} fontWeight={600} letterSpacing="0.09em" fill="#5B5B54">
          {t("viz.scatter.yUp")}
        </text>
        <text x={mid} y={size - pad + 22} textAnchor="middle"
              fontFamily={LABEL} fontSize={11} fontWeight={600} letterSpacing="0.09em" fill="#5B5B54">
          {t("viz.scatter.yDown")}
        </text>

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
                      stroke={active ? "#0A0A0A" : "#FCFCFA"} strokeWidth={active ? 2 : 1.5}
                      opacity={hover && !active ? 0.3 : 0.92}
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
          const textX = boxX + 12;
          return (
            <g pointerEvents="none">
              <rect x={boxX} y={cy - 34} width={220} height={58} rx={8} fill="#0A0A0A" opacity={0.94} />
              <text x={textX} y={cy - 16} fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={13} fill="#FFFFFF">
                {highlighted.name}
              </text>
              <text x={textX} y={cy + 2} fontFamily={LABEL} fontSize={10} fill="#6BB4F0" letterSpacing="0.06em">
                {highlighted.factionShortName ?? "—"} · {highlighted.totalComparableVotes} {t("viz.scatter.tooltipVotes")}
              </text>
              <text x={textX} y={cy + 18} fontFamily={LABEL} fontSize={10} fill="#FFFFFF" opacity={0.7} letterSpacing="0.06em">
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

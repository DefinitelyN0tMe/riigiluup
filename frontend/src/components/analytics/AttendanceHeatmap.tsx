import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { AttendanceMatrix } from "../../api/analytics";

export default function AttendanceHeatmap({ data }: { data: AttendanceMatrix }) {
  const { t } = useTranslation();
  if (!data.members.length || !data.sittings.length) {
    return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;
  }
  const cols = data.sittings;
  // Sort by attendance rate desc: best on top, worst at the bottom.
  // Server already filters MPs with too few recorded checks.
  const rows = [...data.members].sort((a, b) => {
    const ra = a.totalChecks ? a.presentCount / a.totalChecks : 0;
    const rb = b.totalChecks ? b.presentCount / b.totalChecks : 0;
    return rb - ra;
  });
  // Preserve original index-to-cells mapping after sort.
  const origIdx = new Map(data.members.map((m, i) => [m.slug, i]));
  const cell = 8;
  const rowH = 14;
  const nameW = 180;
  const dateH = 36;

  const w = nameW + cols.length * cell + 56;
  const h = dateH + rows.length * rowH + 16;

  return (
    <div className="max-h-[70vh] overflow-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0 border border-rule/60 rounded-lg bg-white/40">
      <svg role="img" aria-label={t("viz.a11y.attendance", { mps: rows.length, checks: cols.length, defaultValue: "MP × attendance-check heatmap, {{mps}} MPs across {{checks}} recent attendance checks." })}
           viewBox={`0 0 ${w} ${h}`} className="w-full block" style={{ minWidth: 720 }}>
        {/* Date column labels (every 5th only, rotated) */}
        {cols.map((c, i) => (
          i % 5 === 0 ? (
            <g key={c.sittingExternalId} transform={`translate(${nameW + i * cell + cell / 2}, ${dateH - 4}) rotate(-60)`}>
              <text fontFamily="'JetBrains Mono', monospace" fontSize={9} fill="#7A7A72" textAnchor="start">
                {c.label}
              </text>
            </g>
          ) : null
        ))}

        {rows.map((r, ri) => {
          const y = dateH + ri * rowH;
          const rate = r.totalChecks ? r.presentCount / r.totalChecks : 0;
          const oi = origIdx.get(r.slug) ?? ri;
          return (
            <g key={r.slug}>
              {/* MP name + faction color */}
              <g transform={`translate(${nameW - 8}, ${y + rowH / 2})`}>
                <circle cx={-nameW + 10} cy={0} r={3} fill={r.factionColorHex ?? "#0072CE"} />
                <text textAnchor="end" dominantBaseline="middle" fontFamily="'Bricolage Grotesque', sans-serif" fontSize={10.5} fill="#0A0A0A" fontWeight={600}>
                  {truncate(r.shortName, 22)}
                </text>
              </g>
              {/* Cells */}
              {cols.map((_, ci) => {
                const idx = oi * cols.length + ci;
                const v = data.cells[idx];
                const fill = v === "P" ? "#0072CE" : v === "A" ? "#FF4B3E" : "#F1F0EA";
                return (
                  <rect key={ci} x={nameW + ci * cell} y={y + 2} width={cell - 1} height={rowH - 4}
                        fill={fill} />
                );
              })}
              {/* Rate at right */}
              <text x={nameW + cols.length * cell + 8} y={y + rowH / 2} dominantBaseline="middle"
                    fontFamily="'JetBrains Mono', monospace" fontSize={9.5} fill={rate < 0.7 ? "#FF4B3E" : "#0A0A0A"} fontWeight={700}>
                {Math.round(rate * 100)}%
              </text>
            </g>
          );
        })}
      </svg>

      {/* Clickable MP names hidden overlay (SVG a tags via foreignObject) — replace with a simple below-list for clarity */}
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-4 text-center">
        <span className="inline-block w-3 h-3 bg-blue mr-1.5 align-middle" /> {t("viz.attendance.present")}
        <span className="inline-block w-3 h-3 bg-hot mr-1.5 ml-4 align-middle" /> {t("viz.attendance.absent")}
        <span className="inline-block w-3 h-3 bg-[#F1F0EA] mr-1.5 ml-4 align-middle" /> {t("viz.attendance.noRecord")}
        <span className="text-blue ml-4">· {t("viz.attendance.footer", { count: cols.length, mps: rows.length })}</span>
      </p>
      <details className="mt-4">
        <summary className="cursor-pointer font-mono text-[11px] tracking-[0.14em] uppercase text-blue hover:underline">{t("viz.attendance.openList")}</summary>
        <ul className="mt-3 grid grid-cols-2 md:grid-cols-4 gap-x-4 gap-y-1 text-sm">
          {rows.map((r) => (
            <li key={r.slug}>
              <Link to={`/politicians/${encodeURIComponent(r.slug)}`} className="hover:underline text-ink">
                {r.shortName}
              </Link>
              <span className="text-muted font-mono text-[10px] ml-1">{r.presentCount}/{r.totalChecks}</span>
            </li>
          ))}
        </ul>
      </details>
    </div>
  );
}

function truncate(s: string, n: number) { return s.length > n ? s.slice(0, n - 1) + "…" : s; }

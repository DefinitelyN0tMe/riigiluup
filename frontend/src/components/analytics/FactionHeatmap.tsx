import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { FactionAgreementMatrix } from "../../api/analytics";

/**
 * Party × party agreement matrix. Cell intensity = agreement rate.
 * Diagonal shows total votes where that faction had a clear majority.
 * Off-diagonal cells are clickable → /votes with faction-pair disagreement filter.
 */
export default function FactionHeatmap({ data }: { data: FactionAgreementMatrix }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const n = data.factions.length;
  if (n === 0) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  function openPair(i: number, j: number) {
    if (i === j) return;
    const a = data.factions[i];
    const b = data.factions[j];
    const params = new URLSearchParams();
    params.set("factionA", a.externalId);
    params.set("factionB", b.externalId);
    params.set("factionAName", a.shortName);
    params.set("factionBName", b.shortName);
    navigate(`/votes?${params.toString()}`);
  }

  const cell = 68;      // desktop
  const gutter = 140;   // room for labels (longer full party names)
  const size = n * cell + gutter;

  const heat = (v: number | null) => {
    if (v == null) return "#F1F0EA";
    // 0..1 → white → blue-glow → blue → blue-deep
    const t = Math.max(0, Math.min(1, v));
    if (t < 0.5) {
      const k = t * 2;
      return interpolate("#F1F0EA", "#6BB4F0", k);
    }
    return interpolate("#6BB4F0", "#003E7E", (t - 0.5) * 2);
  };
  const textColor = (v: number | null) => (v != null && v > 0.55 ? "#ffffff" : "#0A0A0A");

  return (
    <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
      <svg role="group" aria-label={t("viz.a11y.factionAgreement", { n, totalVotes: data.totalVotesConsidered, defaultValue: "Faction-vs-faction agreement matrix, {{n}}×{{n}}, based on {{totalVotes}} votes." })}
           viewBox={`0 0 ${size} ${size}`} className="w-full max-w-[720px] mx-auto block" style={{ minWidth: 520 }}>
        {/* column labels top */}
        {data.factions.map((f, j) => (
          <g key={`c-${j}`} transform={`translate(${gutter + j * cell + cell / 2}, ${gutter - 8}) rotate(-45)`}>
            <title>{f.name}</title>
            <text textAnchor="start" fontFamily="'JetBrains Mono', monospace" fontSize={10} letterSpacing="0.08em" fill="#0A0A0A" fontWeight={700}>
              {f.shortName}
            </text>
          </g>
        ))}
        {/* row labels left — colour dot sits in the 24px gap between text and matrix
             so it never overprints letters (was: cx=-12 fell inside short labels). */}
        {data.factions.map((f, i) => (
          <g key={`r-${i}`} transform={`translate(${gutter - 24}, ${gutter + i * cell + cell / 2})`}>
            <title>{f.name}</title>
            <text textAnchor="end" dominantBaseline="middle" fontFamily="'JetBrains Mono', monospace" fontSize={11} letterSpacing="0.08em" fill="#0A0A0A" fontWeight={700}>
              {f.shortName}
            </text>
            <circle r={4} cx={12} cy={0} fill={f.colorHex ?? "#0072CE"} />
          </g>
        ))}
        {/* cells */}
        {data.matrix.map((row, i) =>
          row.map((v, j) => {
            const x = gutter + j * cell;
            const y = gutter + i * cell;
            const isDiag = i === j;
            const label = v == null ? "—" : `${Math.round(v * 100)}%`;
            const clickable = !isDiag;
            const cellLabel = t("viz.agreement.openCell", { a: data.factions[i].shortName, b: data.factions[j].shortName });
            return (
              <g key={`${i}-${j}`}
                 style={{ cursor: clickable ? "pointer" : "default" }}
                 onClick={() => openPair(i, j)}
                 tabIndex={clickable ? 0 : undefined}
                 role={clickable ? "link" : undefined}
                 aria-label={clickable ? cellLabel : undefined}
                 onKeyDown={clickable ? (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openPair(i, j); } } : undefined}>
                <title>{clickable ? cellLabel : label}</title>
                <rect x={x + 2} y={y + 2} width={cell - 4} height={cell - 4}
                      fill={heat(v)} stroke={isDiag ? "#0A0A0A" : "transparent"} strokeWidth={isDiag ? 1.5 : 0} rx={4} />
                <text x={x + cell / 2} y={y + cell / 2 - 2} textAnchor="middle" dominantBaseline="middle"
                      fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={16} letterSpacing="-0.02em"
                      fill={textColor(v)}>
                  {label}
                </text>
                <text x={x + cell / 2} y={y + cell / 2 + 14} textAnchor="middle" dominantBaseline="middle"
                      fontFamily="'JetBrains Mono', monospace" fontSize={9} fill={textColor(v)} opacity={0.7}>
                  n={data.support[i]?.[j] ?? 0}
                </text>
              </g>
            );
          })
        )}
      </svg>

      {/* Legend */}
      <div className="mt-6 flex flex-wrap justify-center items-center gap-3 sm:gap-5 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#F1F0EA" }} /> 0%</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#6BB4F0" }} /> 50%</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#003E7E" }} /> 100%</span>
        <span className="text-blue">· {t("viz.agreement.basedOn", { count: data.totalVotesConsidered })}</span>
      </div>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center mt-2">
        ↑ {t("viz.agreement.clickHint")}
      </p>
    </div>
  );
}

/** Linear color interpolation between two hex colors. */
function interpolate(a: string, b: string, t: number): string {
  const [ar, ag, ab] = hex(a);
  const [br, bg, bb] = hex(b);
  const r = Math.round(ar + (br - ar) * t);
  const g = Math.round(ag + (bg - ag) * t);
  const c = Math.round(ab + (bb - ab) * t);
  return `rgb(${r},${g},${c})`;
}
function hex(s: string): [number, number, number] {
  const v = s.replace("#", "");
  return [parseInt(v.slice(0, 2), 16), parseInt(v.slice(2, 4), 16), parseInt(v.slice(4, 6), 16)];
}

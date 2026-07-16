import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { VoteTimingHeatmap as Data } from "../../api/analytics";

export default function VoteTimingHeatmap({ data }: { data: Data }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  function openCell(dow: number, hour: number, count: number) {
    if (count === 0) return;
    const params = new URLSearchParams();
    params.set("dow", String(dow));
    params.set("hour", String(hour));
    navigate(`/votes?${params.toString()}`);
  }
  const DAYS = [t("viz.timing.d1"), t("viz.timing.d2"), t("viz.timing.d3"), t("viz.timing.d4"), t("viz.timing.d5"), t("viz.timing.d6"), t("viz.timing.d7")];
  if (!data.totalVotes) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;
  const cell = 22;
  const hdr = 30;
  const rowW = 46;
  const w = rowW + 24 * cell + 20;
  const h = hdr + 7 * cell + 40;

  const heat = (v: number) => {
    if (v === 0) return "#F1F0EA";
    const t = Math.min(1, v / (data.maxCell || 1));
    if (t < 0.5) return interp("#F1F0EA", "#6BB4F0", t * 2);
    return interp("#6BB4F0", "#003E7E", (t - 0.5) * 2);
  };

  return (
    <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
      <svg role="group" aria-label={t("viz.a11y.voteTiming", { total: data.totalVotes, defaultValue: "Weekday × hour heatmap of {{total}} vote events, Europe/Tallinn." })}
           viewBox={`0 0 ${w} ${h}`} className="w-full block" style={{ minWidth: 560 }}>
        {Array.from({ length: 24 }).map((_, i) => (
          <text key={i} x={rowW + i * cell + cell / 2} y={hdr - 8} textAnchor="middle"
                fontFamily="'JetBrains Mono', monospace" fontSize={9} fill="#7A7A72"
                fontWeight={i === 9 || i === 15 ? 700 : 400}>
            {i.toString().padStart(2, "0")}
          </text>
        ))}
        {DAYS.map((d, di) => (
          <g key={d}>
            <text x={rowW - 8} y={hdr + di * cell + cell / 2} textAnchor="end" dominantBaseline="middle"
                  fontFamily="'JetBrains Mono', monospace" fontSize={11} fill="#0A0A0A" fontWeight={700}
                  letterSpacing="0.08em">
              {d.toUpperCase()}
            </text>
            {data.cells[di]?.map((v, hi) => {
              const clickable = v > 0;
              const cellLabel = t("viz.timing.openCell", { count: v, day: d, hour: hi });
              return (
              <g key={hi} style={{ cursor: clickable ? "pointer" : "default" }} onClick={() => openCell(di, hi, v)}
                 tabIndex={clickable ? 0 : undefined}
                 role={clickable ? "link" : undefined}
                 aria-label={clickable ? cellLabel : undefined}
                 onKeyDown={clickable ? (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openCell(di, hi, v); } } : undefined}>
                <title>{clickable ? cellLabel : ""}</title>
                <rect x={rowW + hi * cell + 1} y={hdr + di * cell + 1} width={cell - 2} height={cell - 2}
                      rx={2} fill={heat(v)} />
                {v > 0 && v > data.maxCell * 0.25 && (
                  <text x={rowW + hi * cell + cell / 2} y={hdr + di * cell + cell / 2}
                        textAnchor="middle" dominantBaseline="middle"
                        fontFamily="'JetBrains Mono', monospace" fontSize={8} fontWeight={700}
                        fill={v > data.maxCell * 0.5 ? "#FFFFFF" : "#0A0A0A"}>
                    {v}
                  </text>
                )}
              </g>
              );
            })}
          </g>
        ))}
      </svg>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-4 text-center">
        {t("viz.timing.footer", { votes: data.totalVotes, max: data.maxCell })}
      </p>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center mt-1">
        ↑ {t("viz.timing.clickHint")}
      </p>
    </div>
  );
}

function interp(a: string, b: string, t: number) {
  const [ar, ag, ab] = hex(a); const [br, bg, bb] = hex(b);
  return `rgb(${Math.round(ar + (br - ar) * t)},${Math.round(ag + (bg - ag) * t)},${Math.round(ab + (bb - ab) * t)})`;
}
function hex(s: string): [number, number, number] {
  const v = s.replace("#", "");
  return [parseInt(v.slice(0, 2), 16), parseInt(v.slice(2, 4), 16), parseInt(v.slice(4, 6), 16)];
}

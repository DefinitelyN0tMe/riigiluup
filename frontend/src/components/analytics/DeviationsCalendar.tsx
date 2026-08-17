import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { MpDeviationsTimeline } from "../../api/analytics";
import { formatDecimal } from "../../lib/formatNumber";
import { formatDate } from "../../lib/formatDate";

/**
 * GitHub-style contribution heatmap of an MP's deviations from their faction's majority.
 * Weeks × days grid, cell intensity = deviation count that day.
 * Empty days (no eligible vote that day) render as light grey placeholders.
 */
export default function DeviationsCalendar({ data }: { data: MpDeviationsTimeline }) {
  const { t, i18n } = useTranslation();

  const grid = useMemo(() => {
    // Build weeks[] where weeks[w][d] = cell for that day (or null)
    const from = new Date(data.rangeFrom + "T00:00:00Z");
    const to = new Date(data.rangeTo + "T00:00:00Z");
    // Align "from" to Monday of its week
    const start = new Date(from);
    // getUTCDay() returns 0=Sun..6=Sat; want Mon=0..Sun=6
    const offset = (start.getUTCDay() + 6) % 7;
    start.setUTCDate(start.getUTCDate() - offset);

    const byDate = new Map<string, { deviations: number; eligible: number }>();
    for (const cell of data.days) byDate.set(cell.date, { deviations: cell.deviations, eligible: cell.eligible });

    const weeks: ({ date: string; deviations: number; eligible: number } | null)[][] = [];
    for (let d = new Date(start); d <= to; d.setUTCDate(d.getUTCDate() + 7)) {
      const week: ({ date: string; deviations: number; eligible: number } | null)[] = [];
      for (let i = 0; i < 7; i++) {
        const day = new Date(d);
        day.setUTCDate(d.getUTCDate() + i);
        if (day < from || day > to) { week.push(null); continue; }
        const iso = day.toISOString().slice(0, 10);
        const cell = byDate.get(iso);
        week.push(cell ? { date: iso, ...cell } : { date: iso, deviations: 0, eligible: 0 });
      }
      weeks.push(week);
    }
    return weeks;
  }, [data]);

  const maxDeviations = useMemo(
    () => Math.max(1, ...data.days.map((d) => d.deviations)),
    [data.days]
  );

  function cellFill(cell: { deviations: number; eligible: number } | null) {
    if (!cell) return "transparent";
    if (cell.eligible === 0) return "#EFEDE3";      // no data that day
    // Clearly green, not pastel: ΔE to the no-data gray must stay ≥15 or the two
    // states read identical (validated with the CVD palette checker).
    if (cell.deviations === 0) return "#8BCE7F";    // eligible but no deviation → green
    const t = Math.min(1, cell.deviations / maxDeviations);
    // 1 dev → light red, more → deeper hot
    if (t < 0.34) return "#FFC5C1";
    if (t < 0.67) return "#FF8078";
    return "#FF4B3E";
  }

  const cellSize = 12;
  const gap = 2;
  const dayLabelW = 24;
  const monthLabelH = 16;
  const width = dayLabelW + grid.length * (cellSize + gap);
  const height = monthLabelH + 7 * (cellSize + gap);

  const dayLabels = [t("viz.timing.d1"), t("viz.timing.d3"), t("viz.timing.d5")];

  // Month labels — place at first week of each month
  const monthLabels: { x: number; label: string }[] = [];
  let prevMonth = -1;
  grid.forEach((week, wi) => {
    const firstCell = week.find((c) => c !== null);
    if (!firstCell) return;
    const dt = new Date(firstCell.date + "T00:00:00Z");
    const m = dt.getUTCMonth();
    if (m !== prevMonth) {
      prevMonth = m;
      monthLabels.push({
        x: dayLabelW + wi * (cellSize + gap),
        label: dt.toLocaleDateString(i18n.resolvedLanguage, { month: "short" }),
      });
    }
  });

  return (
    <div>
      <div className="flex items-baseline justify-between gap-3 flex-wrap mb-4">
        <div>
          <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
            {t("viz.deviations.subtitle", {
              from: formatDate(data.rangeFrom, { month: "short", year: "2-digit" }),
              to: formatDate(data.rangeTo, { month: "short", year: "2-digit" }),
            })}
          </div>
          <div className="font-serif italic text-[16px] text-ink-2 mt-1">
            {t("viz.deviations.summary", {
              dev: data.totalDeviations,
              elig: data.totalEligible,
              pct: data.totalEligible === 0 ? "0" : formatDecimal(100 * data.totalDeviations / data.totalEligible),
            })}
          </div>
        </div>
      </div>

      <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
        <svg role="img" aria-label={t("viz.a11y.deviationsCalendar", { defaultValue: "Calendar heatmap of MP's deviations from own-faction majority." })}
             viewBox={`0 0 ${width} ${height}`} className="block" style={{ minWidth: 640, height: height + 8 }}>
          {monthLabels.map((m, i) => (
            <text key={i} x={m.x} y={12} fontFamily="'JetBrains Mono', monospace" fontSize={9}
                  fill="#7A7A72" letterSpacing="0.08em">
              {m.label}
            </text>
          ))}
          {[0, 2, 4].map((dow, i) => (
            <text key={dow} x={0} y={monthLabelH + dow * (cellSize + gap) + cellSize - 2}
                  fontFamily="'JetBrains Mono', monospace" fontSize={9} fill="#7A7A72" letterSpacing="0.08em">
              {dayLabels[i]}
            </text>
          ))}
          {grid.map((week, wi) =>
            week.map((cell, di) => {
              const x = dayLabelW + wi * (cellSize + gap);
              const y = monthLabelH + di * (cellSize + gap);
              const fill = cellFill(cell);
              const isDev = cell && cell.deviations > 0;
              return (
                <g key={`${wi}-${di}`}>
                  {cell && (
                    <title>
                      {formatDate(cell.date + "T00:00:00Z", {
                        day: "2-digit", month: "long", year: "numeric",
                      })} · {cell.deviations}/{cell.eligible} {t("viz.deviations.tooltipUnit")}
                    </title>
                  )}
                  <rect x={x} y={y} width={cellSize} height={cellSize} rx={2}
                        fill={fill} stroke={isDev ? "#B33028" : "none"} strokeWidth={isDev ? 0.5 : 0} />
                </g>
              );
            })
          )}
        </svg>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-3 font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm" style={{ background: "#EFEDE3" }} /> {t("viz.deviations.legendNoData")}</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm" style={{ background: "#8BCE7F" }} /> {t("viz.deviations.legendWithMajority")}</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm" style={{ background: "#FFC5C1" }} /> {t("viz.deviations.legend1dev")}</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm" style={{ background: "#FF4B3E" }} /> {t("viz.deviations.legendManyDev")}</span>
      </div>
    </div>
  );
}

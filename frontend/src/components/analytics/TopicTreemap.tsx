import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { TopicTreemap as Data } from "../../api/analytics";

/** Squarified treemap (simplified) — sorted by billCount, laid out in horizontal strips. */
export default function TopicTreemap({ data, height = 440 }: { data: Data; height?: number }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  if (!data.items.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  function openTopic(edid: number, label: string) {
    const params = new URLSearchParams();
    params.set("topicEdid", String(edid));
    params.set("topicLabel", label);
    navigate(`/legislation?${params.toString()}`);
  }

  const width = 960;
  const items = [...data.items].sort((a, b) => b.billCount - a.billCount);
  const total = items.reduce((s, i) => s + i.billCount, 0) || 1;

  // Simple strip treemap: group items into rows, each row height proportional to sum
  const targetRows = Math.max(3, Math.round(Math.sqrt(items.length / 3)));
  const perRow = Math.ceil(items.length / targetRows);
  const rows: { items: typeof items; sum: number }[] = [];
  for (let i = 0; i < items.length; i += perRow) {
    const group = items.slice(i, i + perRow);
    rows.push({ items: group, sum: group.reduce((s, it) => s + it.billCount, 0) });
  }
  const totalSum = rows.reduce((s, r) => s + r.sum, 0) || 1;

  const rects: {
    x: number; y: number; w: number; h: number;
    item: (typeof items)[number]; color: string;
  }[] = [];
  let cy = 0;
  rows.forEach((row) => {
    const rowH = (row.sum / totalSum) * height;
    let cx = 0;
    row.items.forEach((it) => {
      const rowSum = row.sum || 1;
      const rw = (it.billCount / rowSum) * width;
      const adoption = it.billCount ? it.adoptedCount / it.billCount : 0;
      // color: blue-glow for low adoption, blue for mid, deep for high
      const color = adoption < 0.25 ? "#B7D8F2" : adoption < 0.5 ? "#6BB4F0" : adoption < 0.75 ? "#0072CE" : "#003E7E";
      rects.push({ x: cx, y: cy, w: rw, h: rowH, item: it, color });
      cx += rw;
    });
    cy += rowH;
  });

  return (
    <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
      <svg role="group" aria-label={t("viz.a11y.topicTreemap", { total: data.totalBills, defaultValue: "Treemap of policy topics, sized by bill count, {{total}} bills total." })}
           viewBox={`0 0 ${width} ${height}`} className="w-full block" style={{ minWidth: 640 }}>
        {rects.map((r, i) => {
          const showTitle = r.w > 90 && r.h > 40;
          const showAdopt = r.w > 60 && r.h > 24;
          const tileLabel = t("viz.topics.openTile", { count: r.item.billCount, label: r.item.label });
          return (
            <g key={i} style={{ cursor: "pointer" }} onClick={() => openTopic(r.item.edid, r.item.label)}
               tabIndex={0} role="link" aria-label={tileLabel}
               onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openTopic(r.item.edid, r.item.label); } }}>
              <title>{tileLabel}</title>
              <rect x={r.x + 1} y={r.y + 1} width={r.w - 2} height={r.h - 2}
                    fill={r.color} rx={4} />
              {showTitle && (
                <foreignObject x={r.x + 8} y={r.y + 8} width={r.w - 16} height={r.h - 16}>
                  <div style={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between",
                                fontFamily: "'Bricolage Grotesque', sans-serif" }}>
                    <div style={{
                      fontWeight: 700, fontSize: Math.min(18, Math.max(11, r.w / 12)),
                      lineHeight: 1.15, letterSpacing: "-0.02em",
                      color: "#FFFFFF", overflow: "hidden", display: "-webkit-box",
                      WebkitLineClamp: Math.max(1, Math.floor((r.h - 40) / 18)), WebkitBoxOrient: "vertical",
                    }}>
                      {r.item.label}
                    </div>
                    <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 10, letterSpacing: "0.06em",
                                  color: "#FFFFFF", opacity: 0.9 }}>
                      <b>{r.item.billCount}</b> {t("viz.topics.billShort")} · <b>{r.item.adoptedCount}</b> {t("viz.topics.adoptedShort")}
                    </div>
                  </div>
                </foreignObject>
              )}
              {!showTitle && showAdopt && (
                <text x={r.x + r.w / 2} y={r.y + r.h / 2} textAnchor="middle" dominantBaseline="middle"
                      fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#FFFFFF">
                  {r.item.billCount}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      <div className="mt-4 flex flex-wrap justify-center items-center gap-3 sm:gap-5 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#B7D8F2" }} /> {t("viz.topics.legend25")}</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#0072CE" }} /> {t("viz.topics.legend50")}</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm" style={{ background: "#003E7E" }} /> {t("viz.topics.legend75")}</span>
        <span className="text-blue">· {t("viz.topics.total", { count: total })}</span>
      </div>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center mt-2">
        ↑ {t("viz.topics.clickHint")}
      </p>
    </div>
  );
}

import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { BillFlow } from "../../api/analytics";

const NODE_LABEL_KEY: Record<string, string> = {
  initiated: "initiated", in_committee: "in_committee",
  first_reading: "first_reading", second_reading: "second_reading",
  third_reading: "third_reading", in_readings: "in_readings",
  submitted: "submitted", adopted: "adopted", rejected: "rejected",
  withdrawn: "withdrawn", other: "other",
};

// Terminal-phase drill-downs — Sankey → /legislation?phase=X.
// Reading nodes are informational (not stored as a phase on the item), so they don't drill.
const NODE_PHASE: Record<string, string | null> = {
  initiated: null,
  in_committee: "IN_COMMITTEE",
  submitted: "SUBMITTED",
  in_readings: "IN_READINGS",
  first_reading: null, second_reading: null, third_reading: null,
  adopted: "ADOPTED",
  rejected: "REJECTED",
  withdrawn: "WITHDRAWN",
  other: "OTHER",
};

export default function BillFlowSankey({ data }: { data: BillFlow }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  if (!data.totalBills) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  function openPhase(id: string) {
    const phase = NODE_PHASE[id];
    if (!phase) return;
    navigate(`/legislation?phase=${encodeURIComponent(phase)}`);
  }

  const columns: string[][] = [
    ["initiated"],
    ["in_committee", "submitted", "first_reading", "other"],
    ["in_readings", "second_reading", "third_reading"],
    ["adopted", "rejected", "withdrawn"],
  ];
  const colorFor = (id: string) => {
    if (id === "adopted") return "#0072CE";
    if (id === "rejected") return "#FF4B3E";
    if (id === "withdrawn") return "#7A7A72";
    if (id === "initiated") return "#0A0A0A";
    if (id.includes("reading")) return "#003E7E";
    return "#6BB4F0";
  };
  const labelOf = (id: string) => {
    const key = NODE_LABEL_KEY[id];
    return key ? t(`viz.billFlow.node.${key}`) : id;
  };

  const w = 900, h = 460;
  const colW = 130;
  const pad = 30;
  const colX = (i: number) => pad + (i * (w - 2 * pad - colW)) / (columns.length - 1);

  const nodeById = new Map(data.nodes.map((n) => [n.id, n]));
  type Rect = { x: number; y: number; w: number; h: number; id: string; label: string; count: number; color: string };
  const rects: Record<string, Rect> = {};

  columns.forEach((col, ci) => {
    const filtered = col.filter((id) => (nodeById.get(id)?.count ?? 0) > 0);
    const totalCount = filtered.reduce((s, id) => s + (nodeById.get(id)?.count ?? 0), 0) || 1;
    const gap = 12;
    const usable = h - 2 * pad - gap * Math.max(0, filtered.length - 1);
    let y = pad;
    filtered.forEach((id) => {
      const n = nodeById.get(id)!;
      const share = (n.count / totalCount) * usable;
      rects[id] = {
        x: colX(ci), y, w: colW, h: Math.max(24, share),
        id, label: labelOf(id), count: n.count, color: colorFor(id),
      };
      y += Math.max(24, share) + gap;
    });
  });

  const ribbons = data.links
    .filter((l) => rects[l.source] && rects[l.target])
    .map((l, idx) => {
      const s = rects[l.source];
      const t2 = rects[l.target];
      const sourceScale = l.count / Math.max(s.count, 1);
      const targetScale = l.count / Math.max(t2.count, 1);
      const sh = s.h * sourceScale;
      const th = t2.h * targetScale;
      const sx = s.x + s.w;
      const tx = t2.x;
      const sy = s.y + s.h / 2;
      const ty = t2.y + t2.h / 2;
      const cx1 = sx + (tx - sx) * 0.5;
      const cx2 = tx - (tx - sx) * 0.5;
      const topPath = `M ${sx} ${sy - sh / 2} C ${cx1} ${sy - sh / 2}, ${cx2} ${ty - th / 2}, ${tx} ${ty - th / 2}`;
      const bottomPath = `L ${tx} ${ty + th / 2} C ${cx2} ${ty + th / 2}, ${cx1} ${sy + sh / 2}, ${sx} ${sy + sh / 2} Z`;
      return { id: `${l.source}-${l.target}-${idx}`, d: topPath + " " + bottomPath, color: colorFor(l.target), count: l.count };
    });

  return (
    <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
      <svg role="group" aria-label={t("viz.a11y.billFlow", { total: data.totalBills, defaultValue: "Bill-flow Sankey diagram tracing {{total}} legislative items through parliamentary phases." })}
           viewBox={`0 0 ${w} ${h}`} className="w-full min-w-[720px]">
        {ribbons.map((r) => (
          <path key={r.id} d={r.d} fill={r.color} fillOpacity={0.28} stroke="none" />
        ))}
        {Object.values(rects).map((r) => {
          const clickable = NODE_PHASE[r.id] != null;
          const nodeLabel = t("viz.billFlow.openNode", { count: r.count, label: r.label });
          return (
          <g key={r.id} style={{ cursor: clickable ? "pointer" : "default" }} onClick={() => openPhase(r.id)}
             tabIndex={clickable ? 0 : undefined}
             role={clickable ? "link" : undefined}
             aria-label={clickable ? nodeLabel : undefined}
             onKeyDown={clickable ? (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openPhase(r.id); } } : undefined}>
            <title>{clickable ? nodeLabel : r.label}</title>
            <rect x={r.x} y={r.y} width={r.w} height={r.h} rx={6} fill={r.color} />
            {r.h > 34 ? (
              <>
                <text x={r.x + r.w / 2} y={r.y + r.h / 2 - 6} textAnchor="middle"
                      fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={14}
                      fill="#FFFFFF" letterSpacing="-0.015em">
                  {r.label}
                </text>
                <text x={r.x + r.w / 2} y={r.y + r.h / 2 + 12} textAnchor="middle"
                      fontFamily="'JetBrains Mono', monospace" fontSize={12} fontWeight={700} fill="#FFFFFF" opacity={0.9}>
                  {r.count}
                </text>
              </>
            ) : (
              <text x={r.x + r.w / 2} y={r.y + r.h / 2} textAnchor="middle" dominantBaseline="middle"
                    fontFamily="'JetBrains Mono', monospace" fontSize={11} fontWeight={700} fill="#FFFFFF">
                {r.label} · {r.count}
              </text>
            )}
          </g>
        );})}
      </svg>

      <div className="mt-4 flex flex-wrap justify-center gap-3 sm:gap-5 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-ink" /> {t("viz.billFlow.legendInitiated")}</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-blue" /> {t("viz.billFlow.legendAdopted")}</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-hot" /> {t("viz.billFlow.legendRejected")}</span>
        <span className="flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-muted" /> {t("viz.billFlow.legendWithdrawn")}</span>
        <span className="text-blue">· {t("viz.billFlow.total", { count: data.totalBills })}</span>
      </div>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center mt-2">
        ↑ {t("viz.billFlow.clickHint")}
      </p>
    </div>
  );
}

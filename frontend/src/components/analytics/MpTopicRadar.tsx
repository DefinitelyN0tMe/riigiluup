import { useTranslation } from "react-i18next";
import type { MpTopicRadar as Data } from "../../api/analytics";

export default function MpTopicRadar({ data }: { data: Data }) {
  const { t } = useTranslation();
  if (!data.topics.length) {
    return (
      <div className="text-muted font-mono text-xs tracking-[0.14em] uppercase py-4">
        {t("viz.topicRadar.empty")}
      </div>
    );
  }
  const size = 300;
  const cx = size / 2, cy = size / 2, R = size / 2 - 30;
  const n = data.topics.length;
  const max = Math.max(...data.topics.map((t) => t.billCount), 1);

  const points = data.topics.map((t, i) => {
    const angle = (i / n) * Math.PI * 2 - Math.PI / 2;
    const r = (t.billCount / max) * R;
    return { x: cx + Math.cos(angle) * r, y: cy + Math.sin(angle) * r, angle, ...t };
  });
  const polygon = points.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ");
  const labelPoints = data.topics.map((t, i) => {
    const angle = (i / n) * Math.PI * 2 - Math.PI / 2;
    return { x: cx + Math.cos(angle) * (R + 14), y: cy + Math.sin(angle) * (R + 14), angle, ...t };
  });

  return (
    <div className="flex flex-col items-center gap-3">
      <svg role="img" aria-label={t("viz.a11y.mpTopicRadar", { defaultValue: "Radar chart of the MP's activity across policy topic areas." })}
           viewBox={`0 0 ${size} ${size}`} className="w-full max-w-[320px]">
        {[0.25, 0.5, 0.75, 1].map((f) => (
          <polygon key={f}
                   points={Array.from({ length: n }).map((_, i) => {
                     const a = (i / n) * Math.PI * 2 - Math.PI / 2;
                     return `${cx + Math.cos(a) * R * f},${cy + Math.sin(a) * R * f}`;
                   }).join(" ")}
                   fill="none" stroke="#E3E3DE" strokeWidth={1} />
        ))}
        {data.topics.map((_, i) => {
          const a = (i / n) * Math.PI * 2 - Math.PI / 2;
          return <line key={i} x1={cx} y1={cy} x2={cx + Math.cos(a) * R} y2={cy + Math.sin(a) * R} stroke="#E3E3DE" />;
        })}
        <polygon points={polygon} fill="#0072CE" fillOpacity={0.24} stroke="#0072CE" strokeWidth={1.5} />
        {points.map((p, i) => (
          <circle key={i} cx={p.x} cy={p.y} r={3} fill="#0072CE" stroke="#FFFFFF" strokeWidth={1.5} />
        ))}
        {labelPoints.map((p, i) => {
          const anchor = p.x < cx - 4 ? "end" : p.x > cx + 4 ? "start" : "middle";
          return (
            <text key={i} x={p.x} y={p.y} textAnchor={anchor} dominantBaseline="middle"
                  fontFamily="'JetBrains Mono', monospace" fontSize={9} fill="#0A0A0A" fontWeight={600}>
              {truncate(p.label, 12)}
            </text>
          );
        })}
      </svg>
      <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted text-center">
        {t("viz.topicRadar.footer", { count: data.totalBills })}
      </div>
    </div>
  );
}
function truncate(s: string, n: number) { return s.length > n ? s.slice(0, n - 1) + "…" : s; }

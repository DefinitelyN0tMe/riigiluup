import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { CoSponsorship } from "../../api/analytics";

/**
 * Force-free circular layout: nodes placed around a circle grouped by faction,
 * edges are quadratic Bezier curves whose curvature depends on faction diff.
 * Simple, deterministic, works with 60+ nodes.
 */
export default function CoSponsorshipGraph({ data }: { data: CoSponsorship }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [hover, setHover] = useState<string | null>(null);
  const size = 720;
  const cx = size / 2;
  const cy = size / 2;
  const rad = size * 0.42;

  // Sort by faction then bills sponsored (bigger = closer to top)
  const nodes = useMemo(() => {
    const grouped = new Map<string, typeof data.nodes>();
    for (const n of data.nodes) {
      const k = n.factionShortName ?? "—";
      const arr = grouped.get(k) ?? [];
      arr.push(n);
      grouped.set(k, arr);
    }
    // Order factions consistently
    const factionOrder = Array.from(grouped.keys()).sort();
    const out: (typeof data.nodes[number] & { angle: number; x: number; y: number })[] = [];
    let counter = 0;
    const total = data.nodes.length || 1;
    for (const fk of factionOrder) {
      const arr = grouped.get(fk)!.sort((a, b) => b.billsSponsored - a.billsSponsored);
      for (const n of arr) {
        const angle = (counter / total) * Math.PI * 2 - Math.PI / 2;
        out.push({ ...n, angle, x: cx + Math.cos(angle) * rad, y: cy + Math.sin(angle) * rad });
        counter++;
      }
    }
    return out;
  }, [data, cx, cy, rad]);

  const posBySlug = useMemo(() => {
    const m = new Map<string, { x: number; y: number; color: string; faction: string | null; name: string }>();
    for (const n of nodes) m.set(n.slug, { x: n.x, y: n.y, color: n.factionColorHex ?? "#0072CE", faction: n.factionShortName, name: n.name });
    return m;
  }, [nodes]);

  const maxWeight = Math.max(...data.edges.map((e) => e.weight), 1);

  if (!nodes.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  return (
    <div>
      <svg role="group" aria-label={t("viz.a11y.cosponsorship", { nodes: data.nodes.length, edges: data.edges.length, defaultValue: "Co-sponsorship network graph with {{nodes}} MPs and {{edges}} shared-bill edges." })}
           viewBox={`0 0 ${size} ${size}`} className="w-full max-w-[780px] mx-auto block">
        <circle cx={cx} cy={cy} r={rad} fill="none" stroke="#E3E3DE" strokeDasharray="2 6" />
        {/* Edges */}
        {data.edges.map((e, i) => {
          const s = posBySlug.get(e.source);
          const t = posBySlug.get(e.target);
          if (!s || !t) return null;
          const sameFaction = s.faction === t.faction;
          const mx = (s.x + t.x) / 2;
          const my = (s.y + t.y) / 2;
          // pull midpoint toward center for cross-faction (nicer arcs)
          const pullT = sameFaction ? 0.15 : 0.55;
          const bx = mx + (cx - mx) * pullT;
          const by = my + (cy - my) * pullT;
          const opacity = Math.min(0.7, 0.15 + (e.weight / maxWeight) * 0.55);
          const width = 0.4 + (e.weight / maxWeight) * 2.2;
          const stroke = sameFaction ? s.color : "#0A0A0A";
          const dim = hover != null && hover !== e.source && hover !== e.target;
          return (
            <path key={i}
                  d={`M ${s.x} ${s.y} Q ${bx} ${by} ${t.x} ${t.y}`}
                  fill="none" stroke={stroke} strokeWidth={width}
                  strokeOpacity={dim ? 0.05 : opacity} />
          );
        })}
        {/* Nodes */}
        {nodes.map((n) => {
          const r = 4 + Math.sqrt(n.billsSponsored) * 1.6;
          const active = hover === n.slug;
          // Flip the tooltip to the left for right-edge nodes so it doesn't clip off-canvas.
          const flip = n.x > size - 220;
          const boxX = flip ? n.x - 210 : n.x + 10;
          const textX = boxX + 10;
          return (
            <g key={n.slug}
               onMouseEnter={() => setHover(n.slug)} onMouseLeave={() => setHover(null)}
               onFocus={() => setHover(n.slug)} onBlur={() => setHover(null)}
               onClick={() => navigate(`/politicians/${encodeURIComponent(n.slug)}`)}
               tabIndex={0} role="link"
               aria-label={t("viz.scatter.openProfile", { name: n.name })}
               onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); navigate(`/politicians/${encodeURIComponent(n.slug)}`); } }}
               style={{ cursor: "pointer" }}>
              <circle cx={n.x} cy={n.y} r={active ? r + 3 : r}
                      fill={n.factionColorHex ?? "#0072CE"}
                      stroke={active ? "#0A0A0A" : "#FFFFFF"} strokeWidth={active ? 2 : 1.2}
                      opacity={hover && !active ? 0.4 : 1} style={{ transition: "r 0.15s, opacity 0.15s" }} />
              {active && (
                <>
                  <rect x={boxX} y={n.y - 26} width={200} height={42} fill="#0A0A0A" opacity={0.92} rx={4} />
                  <text x={textX} y={n.y - 10} fontFamily="'Bricolage Grotesque', sans-serif" fontWeight={700} fontSize={13} fill="#FFFFFF">
                    {n.name}
                  </text>
                  <text x={textX} y={n.y + 5} fontFamily="'JetBrains Mono', monospace" fontSize={10} fill="#6BB4F0" letterSpacing="0.06em">
                    {n.factionShortName ?? "—"} · {n.billsSponsored} {t("viz.cospons.tooltipBills")}
                  </text>
                </>
              )}
            </g>
          );
        })}
        {/* Center caption */}
        <g transform={`translate(${cx}, ${cy})`}>
          <text textAnchor="middle" fontFamily="'Fraunces', serif" fontStyle="italic" fontWeight={300} fontSize={22} letterSpacing="-0.02em" fill="#0072CE">
            {t("viz.cospons.center")}
          </text>
          <text y={22} textAnchor="middle" fontFamily="'JetBrains Mono', monospace" fontSize={10} letterSpacing="0.14em" fill="#7A7A72">
            {t("viz.cospons.bills", { count: data.totalBillsConsidered })}
          </text>
        </g>
      </svg>
      <p className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-3 text-center max-w-2xl mx-auto">
        {t("viz.cospons.note")}
      </p>
    </div>
  );
}

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { AdminUnauthorizedError, fetchAdminAnalytics } from "../../api/admin";
import type { AdminAnalytics, AdminAnalyticsMetric } from "../../types";

type Range = "24h" | "7d" | "30d";
const RANGES: Range[] = ["24h", "7d", "30d"];

const nf = new Intl.NumberFormat("et-EE");

function fmtDuration(seconds: number): string {
  const s = Math.round(seconds);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  return `${m}m ${s % 60}s`;
}

/** Small "+12%" / "−4%" delta vs the previous, equal-length period. */
function Delta({ metric }: { metric: AdminAnalyticsMetric | null }) {
  if (!metric || !metric.prev) return null;
  const pct = ((metric.value - metric.prev) / metric.prev) * 100;
  if (!isFinite(pct) || Math.abs(pct) < 0.5) return null;
  const up = pct > 0;
  return (
    <span className={`font-mono text-[11px] ${up ? "text-emerald-600" : "text-rose-600"}`}>
      {up ? "▲" : "▼"} {Math.abs(pct).toFixed(0)}%
    </span>
  );
}

function StatTile({ label, value, delta, accent }: {
  label: string; value: string; delta?: ReactNode; accent?: boolean;
}) {
  return (
    <div className="border border-rule rounded-[16px] p-3 bg-white">
      <div className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted">{label}</div>
      <div className="flex items-baseline gap-2 mt-1">
        <span className={`text-2xl font-display font-bold ${accent ? "text-blue" : "text-ink"}`}>{value}</span>
        {delta}
      </div>
    </div>
  );
}

/** Umami returns only buckets that have events; zero-fill the whole window so a single
 *  active day/hour renders as one bar among many rather than one full-width block. */
function fillBuckets(data: AdminAnalytics["series"], range: Range): AdminAnalytics["series"] {
  const now = new Date();
  const keys: { key: string; iso: string }[] = [];
  const dayKey = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
  const hourKey = (d: Date) => `${dayKey(d)}-${d.getHours()}`;
  if (range === "24h") {
    for (let i = 23; i >= 0; i--) {
      const d = new Date(now); d.setMinutes(0, 0, 0); d.setHours(d.getHours() - i);
      keys.push({ key: hourKey(d), iso: d.toISOString() });
    }
  } else {
    const days = range === "30d" ? 30 : 7;
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now); d.setHours(0, 0, 0, 0); d.setDate(d.getDate() - i);
      keys.push({ key: dayKey(d), iso: d.toISOString() });
    }
  }
  const agg = new Map<string, { pv: number; s: number }>();
  for (const p of data) {
    const d = new Date(p.t.includes("T") ? p.t : p.t.replace(" ", "T"));
    if (isNaN(d.getTime())) continue;
    const k = range === "24h" ? hourKey(d) : dayKey(d);
    const cur = agg.get(k) ?? { pv: 0, s: 0 };
    cur.pv += p.pageviews; cur.s += p.sessions;
    agg.set(k, cur);
  }
  return keys.map(({ key, iso }) => ({
    t: iso, pageviews: agg.get(key)?.pv ?? 0, sessions: agg.get(key)?.s ?? 0,
  }));
}

/** Bars = pageviews, overlaid line = visits (sessions). One shared count axis. */
function TrendChart({ data: raw, range }: { data: AdminAnalytics["series"]; range: Range }) {
  const { t } = useTranslation();
  const [hover, setHover] = useState<number | null>(null);
  const data = useMemo(() => fillBuckets(raw, range), [raw, range]);
  if (!raw.length) return <div className="text-muted font-mono text-sm py-8 text-center">{t("viz.noData")}</div>;

  const W = 720, H = 200, padL = 8, padR = 8, padT = 16, padB = 22;
  const iw = W - padL - padR, ih = H - padT - padB;
  const max = Math.max(1, ...data.map((d) => Math.max(d.pageviews, d.sessions)));
  const n = data.length;
  const bw = iw / n;
  const x = (i: number) => padL + i * bw;
  const y = (v: number) => padT + ih - (v / max) * ih;

  const fmtX = (ts: string) => {
    const d = new Date(ts.includes("T") ? ts : ts.replace(" ", "T"));
    if (isNaN(d.getTime())) return "";
    return range === "24h"
      ? `${String(d.getHours()).padStart(2, "0")}:00`
      : `${String(d.getDate()).padStart(2, "0")}.${String(d.getMonth() + 1).padStart(2, "0")}`;
  };

  const sessionsLine = data.map((d, i) => `${x(i) + bw / 2},${y(d.sessions)}`).join(" ");
  const ticks = [0, Math.floor(n / 2), n - 1].filter((v, i, a) => a.indexOf(v) === i);
  const hv = hover != null ? data[hover] : null;

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-4 font-mono text-[11px] text-muted">
          <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-[2px] bg-blue inline-block" />{t("admin.analytics.pageviews")}</span>
          <span className="flex items-center gap-1.5"><span className="w-3 h-[2px] bg-ink inline-block" />{t("admin.analytics.visits")}</span>
        </div>
        <div className="font-mono text-[11px] text-ink min-h-[16px]" aria-live="polite">
          {hv && (
            <span>{fmtX(hv.t)} · {nf.format(hv.pageviews)} {t("admin.analytics.pageviews").toLowerCase()} · {nf.format(hv.sessions)} {t("admin.analytics.visits").toLowerCase()}</span>
          )}
        </div>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" role="img" preserveAspectRatio="xMidYMid meet"
           aria-label={t("admin.analytics.chartAria")} className="overflow-visible">
        {data.map((d, i) => {
          const bh = (d.pageviews / max) * ih;
          const active = hover === i;
          return (
            <g key={i} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}>
              <rect x={x(i)} y={padT} width={bw} height={ih} fill="transparent" />
              <rect x={x(i) + Math.min(2, bw * 0.15)} y={y(d.pageviews)}
                    width={Math.max(1, bw - Math.min(4, bw * 0.3))} height={Math.max(0, bh)}
                    rx={Math.min(3, bw / 3)} fill="#0072CE" opacity={active ? 1 : 0.82} />
            </g>
          );
        })}
        <polyline points={sessionsLine} fill="none" stroke="#1B1B1B" strokeWidth={2}
                  strokeLinejoin="round" strokeLinecap="round" opacity={0.85} />
        {ticks.map((i) => (
          <text key={i} x={x(i) + bw / 2} y={H - 6} textAnchor="middle"
                className="fill-muted font-mono" fontSize="10">{fmtX(data[i].t)}</text>
        ))}
      </svg>
    </div>
  );
}

/** Horizontal bar-list (top pages / referrers). */
function BarList({ title, items, empty }: {
  title: string; items: AdminAnalytics["topPages"]; empty: string;
}) {
  const max = Math.max(1, ...items.map((i) => i.count));
  return (
    <div>
      <h3 className="font-mono text-[11px] tracking-[0.12em] uppercase text-muted mb-2">{title}</h3>
      {items.length === 0 ? (
        <p className="text-muted text-sm">{empty}</p>
      ) : (
        <ol className="flex flex-col gap-1.5">
          {items.map((it, i) => (
            <li key={i} className="relative flex items-center justify-between gap-3 px-2.5 py-1.5 rounded-md overflow-hidden">
              <div className="absolute inset-y-0 left-0 bg-blue/10 rounded-md" style={{ width: `${(it.count / max) * 100}%` }} />
              <span className="relative truncate text-[13px] text-ink font-medium" title={it.label}>{it.label}</span>
              <span className="relative font-mono text-[12px] text-muted shrink-0">{nf.format(it.count)}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

export default function AdminAnalyticsSection({ onUnauthorized }: { onUnauthorized: () => void }) {
  const { t } = useTranslation();
  const [range, setRange] = useState<Range>("7d");
  const [data, setData] = useState<AdminAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      setData(await fetchAdminAnalytics(range));
    } catch (e) {
      if (e instanceof AdminUnauthorizedError) onUnauthorized();
      else setErr((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [range, onUnauthorized]);

  useEffect(() => { load(); }, [load]);

  const tiles = useMemo(() => {
    if (!data || !data.configured || data.error) return null;
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <StatTile label={t("admin.analytics.activeNow")} value={nf.format(data.activeVisitors)} accent />
        <StatTile label={t("admin.analytics.visitors")} value={nf.format(data.visitors?.value ?? 0)} delta={<Delta metric={data.visitors} />} />
        <StatTile label={t("admin.analytics.visits")} value={nf.format(data.visits?.value ?? 0)} delta={<Delta metric={data.visits} />} />
        <StatTile label={t("admin.analytics.pageviews")} value={nf.format(data.pageviews?.value ?? 0)} delta={<Delta metric={data.pageviews} />} />
        <StatTile label={t("admin.analytics.bounceRate")} value={data.bounceRate == null ? "—" : `${Math.round(data.bounceRate * 100)}%`} />
        <StatTile label={t("admin.analytics.avgVisit")} value={data.avgVisitSeconds == null ? "—" : fmtDuration(data.avgVisitSeconds)} />
      </div>
    );
  }, [data, t]);

  return (
    <section aria-label="Analytics" className="border border-rule rounded-[22px] p-4 sm:p-5 bg-off/40">
      <div className="flex items-center justify-between flex-wrap gap-3 mb-4">
        <div>
          <h2 className="text-lg font-semibold text-ink">{t("admin.analytics.title")}</h2>
          <p className="text-xs text-muted mt-0.5">{t("admin.analytics.subtitle")}</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="inline-flex rounded-lg border border-rule overflow-hidden">
            {RANGES.map((r) => (
              <button key={r} onClick={() => setRange(r)}
                className={`px-3 py-1.5 text-sm font-mono ${range === r ? "bg-blue text-white" : "bg-white text-ink hover:bg-off"}`}>
                {t(`admin.analytics.range.${r}`)}
              </button>
            ))}
          </div>
          <button onClick={load} className="text-sm text-estonia hover:underline">{t("admin.refresh")}</button>
        </div>
      </div>

      {loading && !data && <p className="text-muted text-sm">{t("admin.running")}</p>}
      {err && <p className="text-rose-600 text-sm" role="alert">{err}</p>}

      {data && !data.configured && (
        <p className="text-muted text-sm">{t("admin.analytics.notConfigured")}</p>
      )}
      {data && data.configured && data.error && (
        <p className="text-rose-600 text-sm" role="alert">{t("admin.analytics.fetchError")}: {data.error}</p>
      )}

      {data && data.configured && !data.error && (
        <div className="space-y-5">
          {tiles}
          <div className="bg-white border border-rule rounded-[16px] p-4">
            <TrendChart data={data.series} range={range} />
          </div>
          <div className="grid md:grid-cols-2 gap-5">
            <BarList title={t("admin.analytics.topPages")} items={data.topPages} empty={t("viz.noData")} />
            <BarList title={t("admin.analytics.topReferrers")} items={data.topReferrers} empty={t("viz.noData")} />
          </div>
        </div>
      )}
    </section>
  );
}

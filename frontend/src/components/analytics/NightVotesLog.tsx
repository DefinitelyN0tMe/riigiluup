import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { NightVotes } from "../../api/analytics";

/**
 * "Night votes" investigative log — votes started outside conventional working hours
 * (08:00–22:00 Europe/Tallinn) or on weekends. Shows overall metric + editorial list.
 */
export default function NightVotesLog({ data }: { data: NightVotes }) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  if (data.totalVotes === 0) {
    return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;
  }

  const dayNames = [t("viz.timing.d1"), t("viz.timing.d2"), t("viz.timing.d3"), t("viz.timing.d4"),
                    t("viz.timing.d5"), t("viz.timing.d6"), t("viz.timing.d7")];
  const nightPct = (data.nightRatio * 100).toFixed(1);
  const openBucketFilter = (params: URLSearchParams) => navigate(`/votes?${params.toString()}`);

  return (
    <div className="flex flex-col gap-8">
      {/* Metric cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-5">
        <MetricCard
          label={t("viz.nightVotes.metrics.total")}
          value={String(data.totalVotes)}
          hint={t("viz.nightVotes.metrics.totalHint")}
          tone="neutral"
          onClick={() => openBucketFilter(new URLSearchParams({ type: "OPEN" }))}
        />
        <MetricCard
          label={t("viz.nightVotes.metrics.night")}
          value={String(data.nightVotes)}
          hint={t("viz.nightVotes.metrics.nightHint", { pct: nightPct })}
          tone="hot"
          onClick={() => {
            openBucketFilter(new URLSearchParams({ type: "OPEN", nightOnly: "true" }));
          }}
        />
        <MetricCard
          label={t("viz.nightVotes.metrics.weekend")}
          value={String(data.weekendVotes)}
          hint={t("viz.nightVotes.metrics.weekendHint")}
          tone="amber"
          onClick={() => openBucketFilter(new URLSearchParams({ type: "OPEN", onlyWeekend: "true" }))}
        />
        <MetricCard
          label={t("viz.nightVotes.metrics.late")}
          value={String(data.lateNightVotes)}
          hint={t("viz.nightVotes.metrics.lateHint")}
          tone="hot"
          onClick={() => openBucketFilter(new URLSearchParams({ type: "OPEN", lateOnly: "true" }))}
        />
      </div>

      {/* Hour distribution mini-bars */}
      <div className="bg-white border border-rule rounded-[20px] p-5 sm:p-6">
        <div className="font-mono text-[10px] tracking-[0.2em] uppercase text-muted mb-3">
          {t("viz.nightVotes.distributionLabel", { start: data.windowStartHour, end: data.windowEndHour })}
        </div>
        <div className="flex items-end gap-1 h-24">
          {data.hourDistribution.map((b) => {
            const max = Math.max(...data.hourDistribution.map((x) => x.total), 1);
            const wkdH = (b.weekday / max) * 100;
            const wkeH = (b.weekend / max) * 100;
            const isNight = b.hourOfDay < data.windowStartHour || b.hourOfDay >= data.windowEndHour;
            return (
              <div key={b.hourOfDay} className="flex-1 flex flex-col items-center justify-end gap-0.5 relative group">
                <div className="w-full flex flex-col justify-end items-stretch h-full">
                  {b.weekend > 0 && (
                    <div className="bg-hot" style={{ height: `${wkeH}%` }} title={`${dayNames[5]}/${dayNames[6]}: ${b.weekend}`} />
                  )}
                  {b.weekday > 0 && (
                    <div className={isNight ? "bg-hot/70" : "bg-blue"}
                         style={{ height: `${wkdH}%` }} title={`${b.weekday}`} />
                  )}
                </div>
                <div className={`font-mono text-[8px] ${isNight ? "text-hot-deep font-bold" : "text-muted"}`}>
                  {String(b.hourOfDay).padStart(2, "0")}
                </div>
              </div>
            );
          })}
        </div>
        <div className="flex justify-between font-mono text-[10px] tracking-[0.12em] uppercase text-muted mt-3 pt-3 border-t border-rule">
          <span className="flex items-center gap-2"><span className="w-2 h-2 bg-blue" /> {t("viz.nightVotes.workhours")}</span>
          <span className="flex items-center gap-2"><span className="w-2 h-2 bg-hot/70" /> {t("viz.nightVotes.nightWeekday")}</span>
          <span className="flex items-center gap-2"><span className="w-2 h-2 bg-hot" /> {t("viz.nightVotes.weekendCell")}</span>
        </div>
      </div>

      {/* List of specific night votes */}
      <div>
        <h3 className="font-display font-bold text-[18px] tracking-[-0.015em] mb-3">
          {t("viz.nightVotes.listTitle")}
        </h3>
        {data.items.length === 0 ? (
          <p className="font-serif italic text-muted">{t("viz.nightVotes.emptyList")}</p>
        ) : (
          <ol className="flex flex-col divide-y divide-rule border border-rule rounded-[20px] bg-white overflow-hidden">
            {data.items.map((it) => (
              <li key={it.voteId}>
                <Link to={`/votes/${encodeURIComponent(it.voteId)}`}
                      className="block p-4 sm:p-5 hover:bg-off transition-colors">
                  <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 mb-1">
                    <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
                      {it.startedAt ? new Date(it.startedAt).toLocaleString(i18n.resolvedLanguage, {
                        day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit",
                        timeZone: "Europe/Tallinn"
                      }) : "—"}
                    </span>
                    <span className={`font-mono text-[9px] tracking-[0.16em] uppercase font-bold px-1.5 py-0.5 rounded ${it.lateNight ? "bg-hot text-white" : "bg-ink text-white"}`}>
                      {it.lateNight ? t("viz.nightVotes.tagLateNight") : t("viz.nightVotes.tagNight")}
                    </span>
                    {it.weekend && (
                      <span className="font-mono text-[9px] tracking-[0.16em] uppercase font-bold px-1.5 py-0.5 rounded bg-amber text-ink">
                        {t("viz.nightVotes.tagWeekend")}
                      </span>
                    )}
                    <span className="font-mono text-[10px] tracking-[0.12em] uppercase text-muted">
                      · {dayNames[it.dayOfWeek - 1]} · {String(it.hourOfDay).padStart(2, "0")}:00
                    </span>
                  </div>
                  <div className="font-display font-bold text-[16px] sm:text-[18px] tracking-[-0.02em] leading-tight">
                    {it.description ?? "—"}
                  </div>
                  {it.linkedBillTitle && (
                    <div className="font-serif italic text-[13px] text-ink-2 mt-1 truncate">
                      «{it.linkedBillTitle}»
                    </div>
                  )}
                  <div className="mt-2 flex flex-wrap gap-3 font-mono text-[11px] text-muted">
                    <span><b className="text-blue">{it.forCount}</b> {t("viz.highlights.for")}</span>
                    <span><b className="text-hot-deep">{it.againstCount}</b> {t("viz.highlights.against")}</span>
                    <span>· {t("pages.voteDetail.margin")} <b className="text-ink">±{it.margin}</b></span>
                    {it.voteNumber != null && <span>· #{it.voteNumber}</span>}
                  </div>
                </Link>
              </li>
            ))}
          </ol>
        )}
      </div>
    </div>
  );
}

function MetricCard({
  label, value, hint, tone, onClick,
}: { label: string; value: string; hint: string; tone: "neutral" | "hot" | "amber"; onClick?: () => void }) {
  const valColor = tone === "hot" ? "text-hot" : tone === "amber" ? "text-ink" : "text-ink";
  const bgColor = tone === "amber" ? "bg-amber/10 border-amber/50" : "bg-white border-rule";
  const clickable = !!onClick;
  return (
    <button type="button" onClick={onClick} disabled={!clickable}
            className={`${bgColor} border rounded-[20px] p-5 text-left w-full transition-all ${clickable ? "hover:-translate-y-0.5 hover:border-blue cursor-pointer" : "cursor-default"}`}>
      <div className="font-mono text-[10px] tracking-[0.2em] uppercase text-muted">{label}</div>
      <div className={`font-display font-bold text-[38px] sm:text-[46px] leading-none tracking-[-0.04em] mt-2 ${valColor}`}>{value}</div>
      <div className="font-serif italic text-[13px] text-ink-2 mt-2 leading-snug">{hint}</div>
    </button>
  );
}

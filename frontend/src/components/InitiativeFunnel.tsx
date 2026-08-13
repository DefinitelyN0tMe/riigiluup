import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { InitiativeFunnel as FunnelData } from "../api/initiatives";

// Step key -> where clicking that bar sends the reader.
const STEP_TARGETS: Record<string, string> = {
  targeted: "/initiatives",
  signing: "/initiatives?phase=sign",
  threshold: "/initiatives",
  sent: "/initiatives",
  decided: "/initiatives",
  draftAct: "/initiatives?decision=draft-act-or-national-matter",
};

/**
 * The citizen-initiative funnel: of every initiative aimed at Riigikogu, how many made it
 * through each stage. Deliberately not "fixed" to look monotonic — the source data has two
 * honest anomalies (threshold reached but never sent; sent despite never reaching threshold)
 * and we surface both rather than smoothing them away.
 */
export default function InitiativeFunnel({ data }: { data: FunnelData }) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  if (!data.steps.length) return <div className="text-muted font-mono text-sm">{t("viz.noData")}</div>;

  const targetedCount = data.steps.find((s) => s.key === "targeted")?.count ?? 0;
  const draftActCount = data.steps.find((s) => s.key === "draftAct")?.count ?? 0;

  const sortedDecisions = [...data.decisions].sort((a, b) => b.count - a.count);
  const sortedCommittees = [...data.committees].sort((a, b) => b.count - a.count);
  const maxCommitteeCount = Math.max(...sortedCommittees.map((c) => c.count), 1);
  const maxDecisionCount = Math.max(...sortedDecisions.map((d) => d.count), 1);

  const chartSummary = t("initiatives.funnel.aria", {
    defaultValue:
      "Funnel chart: {{targeted}} initiatives targeted at Riigikogu, {{signing}} in signing, {{threshold}} reached the signature threshold, {{sent}} sent to parliament, {{decided}} decided, {{draftAct}} became a draft act.",
    targeted: targetedCount,
    signing: data.steps.find((s) => s.key === "signing")?.count ?? 0,
    threshold: data.steps.find((s) => s.key === "threshold")?.count ?? 0,
    sent: data.steps.find((s) => s.key === "sent")?.count ?? 0,
    decided: data.steps.find((s) => s.key === "decided")?.count ?? 0,
    draftAct: draftActCount,
  });

  function goToCommittee(c: { slug: string; name: string | null }) {
    const sp = new URLSearchParams();
    sp.set("committee", c.slug);
    if (c.name) sp.set("committeeName", c.name);
    navigate(`/initiatives?${sp.toString()}`);
  }

  return (
    <div>
      <div role="group" aria-label={chartSummary} className="flex flex-col gap-3">
        <ol className="flex flex-col gap-3 list-none p-0 m-0">
          {data.steps.map((s) => {
            const pct = s.shareOfTargeted ?? 0;
            const barPct = s.count > 0 ? Math.max(pct, 1.5) : 0;
            const target = STEP_TARGETS[s.key] ?? "/initiatives";
            const stepLabel = t(`initiatives.funnel.step.${s.key}` as const, { defaultValue: s.key });
            return (
              <li key={s.key}>
                <button
                  type="button"
                  onClick={() => navigate(target)}
                  aria-label={t("initiatives.funnel.openStep", {
                    defaultValue: "{{label}}: {{count}} initiatives, open filtered list",
                    label: stepLabel,
                    count: s.count,
                  })}
                  className="w-full text-left group rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-blue"
                >
                  <div className="flex items-baseline justify-between mb-1 gap-3">
                    <span className="font-mono text-[11px] tracking-[0.1em] uppercase text-ink-2 group-hover:text-blue transition-colors">
                      {stepLabel}
                    </span>
                    <span className="font-mono text-[11px] text-muted whitespace-nowrap">
                      <b className="font-display font-bold text-[16px] text-ink">{s.count.toLocaleString(i18n.resolvedLanguage)}</b>
                      {s.shareOfTargeted != null && <span className="ml-1.5">{s.shareOfTargeted.toFixed(1)}%</span>}
                    </span>
                  </div>
                  <div className="h-7 sm:h-8 rounded-lg bg-off overflow-hidden">
                    <div
                      className="h-full rounded-lg bg-blue group-hover:bg-blue-deep transition-colors"
                      style={{ width: `${barPct}%` }}
                    />
                  </div>
                </button>
              </li>
            );
          })}
        </ol>
      </div>

      {/* Editorial note — the whole reason this feature exists. */}
      <div className="mt-7 sm:mt-9 bg-white border border-rule rounded-[20px] p-5 sm:p-6">
        <p className="font-serif italic text-[16px] sm:text-[18px] text-ink leading-snug">
          {t("initiatives.funnel.note.headline", {
            defaultValue: "Of {{targeted}} citizen initiatives aimed at Riigikogu, {{draftAct}} became a draft act.",
            targeted: targetedCount,
            draftAct: draftActCount,
          })}
        </p>
        <ul className="mt-4 pt-4 border-t border-rule space-y-2.5 font-mono text-[12px] text-ink-2 leading-relaxed list-none p-0">
          <li>
            {t("initiatives.funnel.note.neverSent", {
              defaultValue: "{{count}} initiatives collected the required 1,000 signatures and were never sent to parliament.",
              count: data.reachedThresholdButNeverSent,
            })}
          </li>
          <li className="text-hot-deep">
            {t("initiatives.funnel.note.belowThreshold", {
              defaultValue:
                "{{count}} initiatives were sent to parliament without ever reaching the signature threshold in this data. The source does not explain why — we do not speculate.",
              count: data.sentBelowThreshold,
            })}
          </li>
          {data.medianDaysToDecision != null && (
            <li>
              {t("initiatives.funnel.note.median", {
                defaultValue: "Median time to a decision: {{days}} days (n = {{n}}).",
                days: Math.round(data.medianDaysToDecision),
                n: data.medianSampleSize,
              })}
            </li>
          )}
        </ul>
      </div>

      <div className="mt-6 sm:mt-8 grid gap-6 sm:grid-cols-2">
        <div>
          <h3 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-3">
            {t("initiatives.funnel.decisionsTitle", { defaultValue: "Decisions" })}
          </h3>
          <ul className="flex flex-col gap-2.5 list-none p-0">
            {sortedDecisions.map((d) => {
              const w = Math.max(4, (d.count / maxDecisionCount) * 100);
              return (
                <li key={d.decision}>
                  <button
                    type="button"
                    onClick={() => navigate(`/initiatives?decision=${d.decision}`)}
                    className="w-full text-left group"
                  >
                    <div className="flex items-baseline justify-between font-mono text-[11px] text-ink-2 group-hover:text-blue transition-colors">
                      <span className="truncate">
                        {t(`initiatives.decision.${d.decision}` as const, { defaultValue: d.decision })}
                      </span>
                      <span className="font-bold text-ink shrink-0 ml-2">{d.count}</span>
                    </div>
                    <div className="h-1.5 rounded-full bg-off overflow-hidden mt-1">
                      <div className="h-full bg-blue rounded-full" style={{ width: `${w}%` }} />
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        <div>
          <h3 className="font-mono text-[10px] tracking-[0.16em] uppercase text-muted mb-3">
            {t("initiatives.funnel.committeesTitle", { defaultValue: "Committees" })}
          </h3>
          <ul className="flex flex-col gap-2.5 list-none p-0">
            {sortedCommittees.map((c) => {
              const w = Math.max(4, (c.count / maxCommitteeCount) * 100);
              return (
                <li key={c.slug}>
                  <button type="button" onClick={() => goToCommittee(c)} className="w-full text-left group">
                    <div className="flex items-baseline justify-between font-mono text-[11px] text-ink-2 group-hover:text-blue transition-colors">
                      <span className="truncate">{c.name ?? c.slug}</span>
                      <span className="font-bold text-ink shrink-0 ml-2">{c.count}</span>
                    </div>
                    <div className="h-1.5 rounded-full bg-off overflow-hidden mt-1">
                      <div className="h-full bg-blue rounded-full" style={{ width: `${w}%` }} />
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      </div>
    </div>
  );
}

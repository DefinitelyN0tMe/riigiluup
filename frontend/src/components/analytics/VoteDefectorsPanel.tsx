import { Link } from "react-router-dom";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { VoteDetail, VoteFactionBreakdown, VoteIndividual } from "../../types";

/**
 * "Waterfall" panel for a single vote:
 *   - For each faction: bar split into (with-majority | defector | not-comparable)
 *   - Editorial lines "X REF MPs voted AGAINST while their faction majority voted FOR"
 * Uses only the data already on VoteDetail — no extra API call.
 */
export default function VoteDefectorsPanel({ v }: { v: VoteDetail }) {
  const { t } = useTranslation();

  // 1) Determine per-faction majority from breakdown (largest comparable-choice count)
  type Maj = "FOR" | "AGAINST" | "ABSTAINED" | null;
  const majorityByFactionExt = useMemo(() => {
    const m = new Map<string, { choice: Maj; count: number; comparable: number }>();
    for (const b of v.factionBreakdowns) {
      const trio: [Maj, number][] = [
        ["FOR", b.inFavor],
        ["AGAINST", b.against],
        ["ABSTAINED", b.abstained],
      ];
      let choice: Maj = null;
      let count = 0;
      for (const [c, n] of trio) {
        if (n > count) { count = n; choice = c; }
      }
      const comparable = b.inFavor + b.against + b.abstained;
      // "clear" only if strictly greatest — ties → null
      const tied = trio.filter(([, n]) => n === count).length > 1;
      m.set(b.factionExternalId ?? b.factionName, {
        choice: tied ? null : choice,
        count, comparable,
      });
    }
    return m;
  }, [v.factionBreakdowns]);

  // 2) Bucket individualVotes into with-majority / defectors / not-comparable
  type Grouped = {
    faction: VoteFactionBreakdown;
    majority: Maj;
    withMajority: VoteIndividual[];
    defectors: VoteIndividual[];
    nonComp: VoteIndividual[];
  };
  const grouped: Grouped[] = useMemo(() => {
    return v.factionBreakdowns.map((f) => {
      const key = f.factionExternalId ?? f.factionName;
      const maj = majorityByFactionExt.get(key)?.choice ?? null;
      const rows = v.individualVotes.filter((iv) => (iv.factionExternalId ?? iv.factionName) === key);
      const withMaj: VoteIndividual[] = [];
      const defs: VoteIndividual[] = [];
      const non: VoteIndividual[] = [];
      for (const r of rows) {
        const comparable = r.choice === "FOR" || r.choice === "AGAINST" || r.choice === "ABSTAINED";
        if (!comparable) non.push(r);
        else if (r.choice === maj) withMaj.push(r);
        else defs.push(r);
      }
      return { faction: f, majority: maj, withMajority: withMaj, defectors: defs, nonComp: non };
    });
  }, [v.factionBreakdowns, v.individualVotes, majorityByFactionExt]);

  const anyDefectors = grouped.some((g) => g.defectors.length > 0);
  if (!v.factionBreakdowns.length) return null;

  const choiceLabel = (c: Maj) => {
    if (!c) return "—";
    return t(`choice.${c}` as const, { defaultValue: c });
  };

  return (
    <section aria-label="Faction discipline waterfall" className="bg-white border border-rule rounded-[22px] p-5 sm:p-6 md:p-7">
      <div className="flex items-baseline justify-between flex-wrap gap-2 mb-4">
        <h2 className="font-display font-bold text-[20px] sm:text-[22px] tracking-[-0.02em]">
          {t("viz.defectors.title")}
        </h2>
        <span className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
          {t("viz.defectors.subtitle")}
        </span>
      </div>

      {!anyDefectors && (
        <p className="font-serif italic text-[14px] text-ink-2 mb-4">
          {t("viz.defectors.noDefectors")}
        </p>
      )}

      <ol className="flex flex-col gap-3">
        {grouped.map((g) => {
          const totalRows = g.faction.total || 1;
          const wMajPct = (g.withMajority.length / totalRows) * 100;
          const defPct = (g.defectors.length / totalRows) * 100;
          const nonPct = (g.nonComp.length / totalRows) * 100;
          return (
            <li key={g.faction.factionName} className="border border-rule rounded-[16px] p-4">
              <div className="flex items-baseline justify-between flex-wrap gap-2 mb-2">
                <div>
                  <div className="font-display font-bold text-[15px] tracking-[-0.015em]">{g.faction.factionName}</div>
                  <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-0.5">
                    {t("viz.defectors.majorityChose")}: <span className="text-ink font-bold">{choiceLabel(g.majority)}</span>
                    {" · "}{g.faction.total} {t("viz.highlights.for").replace(/./g, "")}{t("votes.membersShort", { count: g.faction.total })}
                  </div>
                </div>
                <div className="font-mono text-[11px] tracking-[0.06em]">
                  <span className="text-blue"><b className="font-bold">{g.withMajority.length}</b> {t("viz.defectors.withMajority")}</span>
                  {g.defectors.length > 0 && (
                    <> · <span className="text-hot"><b className="font-bold">{g.defectors.length}</b> {t("viz.defectors.defected")}</span></>
                  )}
                  {g.nonComp.length > 0 && (
                    <> · <span className="text-muted"><b className="font-bold">{g.nonComp.length}</b> {t("viz.defectors.nonComparable")}</span></>
                  )}
                </div>
              </div>
              {/* Waterfall bar */}
              <div className="flex h-3 rounded-full overflow-hidden bg-[#F1F0EA] mb-3">
                {wMajPct > 0 && <div className="bg-blue" style={{ width: `${wMajPct}%` }} title={`${g.withMajority.length} ${t("viz.defectors.withMajority")}`} />}
                {defPct > 0 && <div className="bg-hot" style={{ width: `${defPct}%` }} title={`${g.defectors.length} ${t("viz.defectors.defected")}`} />}
                {nonPct > 0 && <div className="bg-[#D8D6CB]" style={{ width: `${nonPct}%` }} title={`${g.nonComp.length} ${t("viz.defectors.nonComparable")}`} />}
              </div>
              {/* Defector names */}
              {g.defectors.length > 0 && (
                <div className="font-serif italic text-[14px] leading-snug text-ink-2">
                  <span className="text-hot font-semibold not-italic">
                    {t("viz.defectors.editorialLine", {
                      count: g.defectors.length,
                      faction: g.faction.factionName,
                      majorityChoice: choiceLabel(g.majority),
                    })}
                    :
                  </span>{" "}
                  {g.defectors.map((iv, i) => (
                    <span key={iv.memberSlug ?? i}>
                      {iv.memberSlug ? (
                        <Link to={`/politicians/${encodeURIComponent(iv.memberSlug)}`}
                              className="text-ink border-b border-hot/40 hover:border-hot pb-0.5">
                          {iv.memberFullName}
                        </Link>
                      ) : (
                        <span>{iv.memberFullName}</span>
                      )}
                      <span className="text-muted font-mono not-italic text-[11px] ml-1">
                        ({choiceLabel(iv.choice as Maj)})
                      </span>
                      {i < g.defectors.length - 1 && <span>, </span>}
                    </span>
                  ))}
                </div>
              )}
            </li>
          );
        })}
      </ol>

      <div className="mt-4 pt-3 border-t border-rule flex flex-wrap gap-4 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm bg-blue" /> {t("viz.defectors.withMajority")}</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm bg-hot" /> {t("viz.defectors.defected")}</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm bg-[#D8D6CB]" /> {t("viz.defectors.nonComparable")}</span>
      </div>
    </section>
  );
}

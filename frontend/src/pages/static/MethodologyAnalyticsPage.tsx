import { useTranslation } from "react-i18next";
import type React from "react";

/**
 * Analytics methodology — every computed metric on /analytics with its exact formula,
 * inputs, exclusions and known limits. Formulas kept in a language-neutral math/SQL
 * notation; prose is translated via the "methodology.analytics.*" i18n block.
 */
export default function MethodologyAnalyticsPage() {
  const { t } = useTranslation();
  const K = "methodology.analytics";
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t(`${K}.title`)}</h1>
      <p>
        {t(`${K}.intro`)} <code>backend/src/main/java/com/politico/analytics/AnalyticsService.java</code>.
      </p>

      <hr />

      <h2>{t(`${K}.velocity.heading`)}</h2>
      <Formula>days = accepted_date − initiated_date  (calendar days)</Formula>
      <Formula>median = days[⌊N/2⌋]   ·   p90 = days[⌊N·0.9⌋]   (sorted ascending)</Formula>
      <p>{t(`${K}.velocity.body`)}</p>
      <Note label={t("methodology.analytics.noteLabel")}>{t(`${K}.velocity.note`)}</Note>

      <h2>{t(`${K}.flow.heading`)}</h2>
      <Formula>node.count(phase) = COUNT(*) FROM legislative_item WHERE phase = ?</Formula>
      <Formula>
        node.count(reading) = COUNT(DISTINCT legislative_item_id) FROM legislative_stage WHERE reading_code = ?
      </Formula>
      <p>{t(`${K}.flow.body`)}</p>
      <Note label={t("methodology.analytics.noteLabel")}>{t(`${K}.flow.note`)}</Note>

      <h2>{t(`${K}.factionAgreement.heading`)}</h2>
      <Formula>
        rate(A,B) = COUNT(votes where A.majority = B.majority){"\n"}
        {"          "}/ COUNT(votes where both A and B had a clear majority)
      </Formula>
      <p>{t(`${K}.factionAgreement.body`)}</p>
      <Note label={t("methodology.analytics.noteLabel")}>{t(`${K}.factionAgreement.note`)}</Note>

      <h2>{t(`${K}.cohesion.heading`)}</h2>
      <Formula>cohesion(faction, 30d) = SUM(MP.choice = faction.majority) / COUNT(*)</Formula>
      <p>{t(`${K}.cohesion.body`)}</p>

      <h2>{t(`${K}.discipline.heading`)}</h2>
      <Formula>
        defection(MP) = COUNT(MP.choice ≠ own_faction.majority){"\n"}
        {"               "}/ COUNT(votes where MP is eligible)
      </Formula>
      <p>{t(`${K}.discipline.body`)}</p>

      <h2>{t(`${K}.attendance.heading`)}</h2>
      <Formula>rate(MP) = present_count / recorded_count</Formula>
      <p>{t(`${K}.attendance.body1`)}</p>
      <p>{t(`${K}.attendance.body2`)}</p>

      <h2>{t(`${K}.timing.heading`)}</h2>
      <Formula>cell(dow, hour) = COUNT(vote_event WHERE started_at is in that bucket)</Formula>
      <p>{t(`${K}.timing.body`)}</p>

      <h2>{t(`${K}.night.heading`)}</h2>
      <Formula>night_share = COUNT(votes with hour ∉ [08, 20)) / COUNT(all votes)</Formula>
      <p>{t(`${K}.night.body`)}</p>

      <h2>{t(`${K}.topics.heading`)}</h2>
      <Formula>bills(topic) = COUNT(DISTINCT legislative_item)  where the bill has that Eurovoc topic</Formula>
      <Formula>adoption(topic) = SUM(bill.phase = ADOPTED) / bills(topic)</Formula>
      <p>{t(`${K}.topics.body`)}</p>

      <h2>{t(`${K}.scatter.heading`)}</h2>
      <Formula>x(MP) = agree_rate_with_coalition_majority − agree_rate_with_opposition_majority</Formula>
      <Formula>y(MP) = 2 · own_faction_alignment_rate − 1</Formula>
      <p>{t(`${K}.scatter.body1`)}</p>
      <p>{t(`${K}.scatter.body2`)}</p>
      <Note label={t("methodology.analytics.noteLabel")}>{t(`${K}.scatter.note`)}</Note>

      <h2>{t(`${K}.cosponsorship.heading`)}</h2>
      <Formula>edge(A, B) = COUNT(DISTINCT bills where both A and B are PLENARY_MEMBER sponsors)</Formula>
      <p>{t(`${K}.cosponsorship.body`)}</p>

      <h2>{t(`${K}.streaks.heading`)}</h2>
      <Formula>streak(MP) = TRUE  iff  present in ALL of the last N attendance checks (N = 8)</Formula>
      <p>{t(`${K}.streaks.body`)}</p>

      <h2>{t(`${K}.tight.heading`)}</h2>
      <Formula>margin = |result_in_favor − result_against|</Formula>
      <p>{t(`${K}.tight.body`)}</p>

      <hr />

      <h2>{t(`${K}.exclusions.heading`)}</h2>
      <ul>
        <li>{t(`${K}.exclusions.item1`)}</li>
        <li>{t(`${K}.exclusions.item2`)}</li>
        <li>{t(`${K}.exclusions.item3`)}</li>
        <li>{t(`${K}.exclusions.item4`)}</li>
      </ul>

      <h2>{t(`${K}.period.heading`)}</h2>
      <p>{t(`${K}.period.body`)}</p>

      <h2>{t(`${K}.repro.heading`)}</h2>
      <p>
        {t(`${K}.repro.body`)}{" "}
        <a href="/corrections" className="text-estonia hover:underline">/corrections</a>.
      </p>
    </article>
  );
}

/** Monospace formula block, consistent with existing methodology pages. */
function Formula({ children }: { children: React.ReactNode }) {
  return (
    <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded whitespace-pre-wrap">
      {children}
    </p>
  );
}

/** Muted caveat block. */
function Note({ children, label }: { children: React.ReactNode; label: string }) {
  return (
    <p className="text-sm bg-amber-50 border-l-4 border-amber-300 pl-3 py-2 pr-2 my-3 text-slate-700">
      <strong className="font-semibold">{label}</strong> {children}
    </p>
  );
}

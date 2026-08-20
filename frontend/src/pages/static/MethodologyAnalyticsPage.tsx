import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import type React from "react";

/**
 * Analytics methodology — every computed metric on /analytics explained in plain
 * language: what it measures, what is left out, and what it does NOT tell you.
 * All prose is translated via the "methodology.analytics.*" i18n block.
 */
export default function MethodologyAnalyticsPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const K = "methodology.analytics";
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      {embedded
        ? <h2 className="text-2xl font-semibold text-ink mt-0">{t(`${K}.title`)}</h2>
        : <h1 className="text-3xl font-semibold text-ink">{t(`${K}.title`)}</h1>}
      <p>{t(`${K}.intro`)}</p>

      <hr />

      <h2>{t(`${K}.velocity.heading`)}</h2>
      <p>{t(`${K}.velocity.body`)}</p>
      <Note label={t(`${K}.noteLabel`)}>{t(`${K}.velocity.note`)}</Note>

      <h2>{t(`${K}.flow.heading`)}</h2>
      <p>{t(`${K}.flow.body`)}</p>
      <Note label={t(`${K}.noteLabel`)}>{t(`${K}.flow.note`)}</Note>

      <h2>{t(`${K}.factionAgreement.heading`)}</h2>
      <p>{t(`${K}.factionAgreement.body`)}</p>
      <Note label={t(`${K}.noteLabel`)}>{t(`${K}.factionAgreement.note`)}</Note>

      <h2>{t(`${K}.cohesion.heading`)}</h2>
      <p>{t(`${K}.cohesion.body`)}</p>

      <h2>{t(`${K}.discipline.heading`)}</h2>
      <p>{t(`${K}.discipline.body`)}</p>

      <h2>{t(`${K}.attendance.heading`)}</h2>
      <p>{t(`${K}.attendance.body1`)}</p>
      <p>{t(`${K}.attendance.body2`)}</p>

      <h2>{t(`${K}.timing.heading`)}</h2>
      <p>{t(`${K}.timing.body`)}</p>

      <h2>{t(`${K}.night.heading`)}</h2>
      <p>{t(`${K}.night.body`)}</p>

      <h2>{t(`${K}.topics.heading`)}</h2>
      <p>{t(`${K}.topics.body`)}</p>

      <h2>{t(`${K}.scatter.heading`)}</h2>
      <p>{t(`${K}.scatter.body1`)}</p>
      <p>{t(`${K}.scatter.body2`)}</p>
      <Note label={t(`${K}.noteLabel`)}>{t(`${K}.scatter.note`)}</Note>

      <h2>{t(`${K}.cosponsorship.heading`)}</h2>
      <p>{t(`${K}.cosponsorship.body`)}</p>

      <h2>{t(`${K}.streaks.heading`)}</h2>
      <p>{t(`${K}.streaks.body`)}</p>

      <h2>{t(`${K}.tight.heading`)}</h2>
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
        <Link to="/corrections" className="text-estonia hover:underline">/corrections</Link>.
      </p>
    </article>
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

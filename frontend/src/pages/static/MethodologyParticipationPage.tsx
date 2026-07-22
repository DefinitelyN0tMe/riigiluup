import { useTranslation } from "react-i18next";

export default function MethodologyParticipationPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      {embedded
        ? <h2 className="text-2xl font-semibold text-ink mt-0">{t("methodology.participation.title")}</h2>
        : <h1 className="text-3xl font-semibold text-ink">{t("methodology.participation.title")}</h1>}

      <h2>{t("methodology.participation.votingHeading")}</h2>
      <p>
        {t("methodology.participation.votingFormula")}
      </p>
      <p>{t("methodology.participation.participatedBody")}</p>
      <p>{t("methodology.participation.excludedNumerator")}</p>
      <ul>
        <li>{t("methodology.participation.excludedN1")}</li>
        <li>{t("methodology.participation.excludedN2")}</li>
        <li>{t("methodology.participation.excludedN3")}</li>
      </ul>
      <p>{t("methodology.participation.excludedBoth")}</p>
      <ul>
        <li>{t("methodology.participation.excludedB1")}</li>
        <li>{t("methodology.participation.excludedB2")}</li>
        <li>{t("methodology.participation.excludedB3")}</li>
      </ul>
      <p>
        {t("methodology.participation.votingSourceLine")}
        <code>/api/votings/{`{uuid}`}</code>.
      </p>

      <h2>{t("methodology.participation.sittingHeading")}</h2>
      <p>
        {t("methodology.participation.sittingFormula")}
      </p>
      <p>{t("methodology.participation.sittingBody")}</p>
      <p>
        {t("methodology.participation.sittingSourceLine")}
        <code>/api/statistics/participations/member/{`{uuid}`}?startDate=…&endDate=…</code>.
      </p>

      <h2>{t("methodology.participation.attendanceHeading")}</h2>
      <p>
        {t("methodology.participation.attendanceFormula")}
      </p>
      <p>
        {t("methodology.participation.attendanceBody1a")}
        <strong>{t("methodology.participation.attendanceBody1b")}</strong>
      </p>
      <p>{t("methodology.participation.attendanceWhyDiff")}</p>
      <p>{t("methodology.participation.attendanceBody2")}</p>
      <p>
        {t("methodology.participation.attendanceSourceLine")}
        <code>/api/votings/{`{uuid}`}</code>.
      </p>

      <h2>{t("methodology.participation.periodHeading")}</h2>
      <p>{t("methodology.participation.periodBody")}</p>
    </article>
  );
}

import { useTranslation } from "react-i18next";

export default function MethodologyParticipationPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("methodology.participation.title")}</h1>

      <h2>{t("methodology.participation.votingHeading")}</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
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

      <h2>{t("methodology.participation.attendanceHeading")}</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        {t("methodology.participation.attendanceFormula")}
      </p>
      <p>
        {t("methodology.participation.attendanceBody1a")}
        <strong>{t("methodology.participation.attendanceBody1b")}</strong>
      </p>
      <p>{t("methodology.participation.attendanceBody2")}</p>
      <p>
        {t("methodology.participation.attendanceSourceLine")}
        <code>/api/statistics/participations/member/{`{uuid}`}?startDate=…&endDate=…</code>.
      </p>

      <h2>{t("methodology.participation.periodHeading")}</h2>
      <p>{t("methodology.participation.periodBody")}</p>
    </article>
  );
}

import { useTranslation } from "react-i18next";

export default function MethodologyAlignmentPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("methodology.alignment.title")}</h1>

      <p>{t("methodology.alignment.intro")}</p>

      <h2>{t("methodology.alignment.formulaHeading")}</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        {t("methodology.alignment.formula")}
      </p>

      <h2>{t("methodology.alignment.eligibleHeading")}</h2>
      <ul>
        <li>{t("methodology.alignment.eligible1")}</li>
        <li>{t("methodology.alignment.eligible2")}</li>
        <li>{t("methodology.alignment.eligible3")}</li>
      </ul>

      <h2>{t("methodology.alignment.matchHeading")}</h2>
      <p>{t("methodology.alignment.matchBody")}</p>

      <h2>{t("methodology.alignment.excludedHeading")}</h2>
      <ul>
        <li>{t("methodology.alignment.excluded1")}</li>
        <li>{t("methodology.alignment.excluded2")}</li>
        <li>{t("methodology.alignment.excluded3")}</li>
      </ul>

      <h2>{t("methodology.alignment.interpretationHeading")}</h2>
      <p>{t("methodology.alignment.interpretation")}</p>
    </article>
  );
}

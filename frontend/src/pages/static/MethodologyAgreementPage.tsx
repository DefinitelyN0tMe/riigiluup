import { useTranslation } from "react-i18next";

export default function MethodologyAgreementPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("methodology.agreement.title")}</h1>

      <p>{t("methodology.agreement.intro")}</p>

      <h2>{t("methodology.agreement.formulaHeading")}</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        {t("methodology.agreement.formula")}
      </p>

      <h2>{t("methodology.agreement.sameDiffHeading")}</h2>
      <ul>
        <li>{t("methodology.agreement.same")}</li>
        <li>{t("methodology.agreement.different")}</li>
      </ul>

      <h2>{t("methodology.agreement.separateHeading")}</h2>
      <ul>
        <li>{t("methodology.agreement.separate")}</li>
      </ul>

      <h2>{t("methodology.agreement.interpretationHeading")}</h2>
      <p>{t("methodology.agreement.interpretation")}</p>
    </article>
  );
}

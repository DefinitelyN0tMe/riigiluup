import { useTranslation } from "react-i18next";

export default function MethodologyAgreementPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      {embedded
        ? <h2 className="text-2xl font-semibold text-ink mt-0">{t("methodology.agreement.title")}</h2>
        : <h1 className="text-3xl font-semibold text-ink">{t("methodology.agreement.title")}</h1>}

      <p>{t("methodology.agreement.intro")}</p>

      <h2>{t("methodology.agreement.formulaHeading")}</h2>
      <p>{t("methodology.agreement.formula")}</p>

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

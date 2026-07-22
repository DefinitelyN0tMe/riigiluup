import { useTranslation } from "react-i18next";

export default function MethodologyAlignmentPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      {embedded
        ? <h2 className="text-2xl font-semibold text-ink mt-0">{t("methodology.alignment.title")}</h2>
        : <h1 className="text-3xl font-semibold text-ink">{t("methodology.alignment.title")}</h1>}

      <p>{t("methodology.alignment.intro")}</p>

      <h2>{t("methodology.alignment.formulaHeading")}</h2>
      <p>{t("methodology.alignment.formula")}</p>

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

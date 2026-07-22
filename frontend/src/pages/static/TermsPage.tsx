import { useTranslation } from "react-i18next";

export default function TermsPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("terms.title")}</h1>
      <h2>{t("terms.licenseHeading")}</h2>
      <p>
        {t("terms.licenseBody1a")}
        <strong>{t("terms.licenseBody1b")}</strong>
        {t("terms.licenseBody1c")}
      </p>
      <p>{t("terms.licenseBody2")}</p>

      <h2>{t("terms.warrantyHeading")}</h2>
      <p>{t("terms.warrantyBody")}</p>

      <h2>{t("terms.neutralityHeading")}</h2>
      <p>{t("terms.neutralityBody")}</p>

      <h2>{t("terms.fairUseHeading")}</h2>
      <p>{t("terms.fairUseBody")}</p>
    </article>
  );
}

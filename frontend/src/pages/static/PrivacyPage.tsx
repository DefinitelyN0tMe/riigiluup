import { useTranslation } from "react-i18next";

export default function PrivacyPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("privacy.title")}</h1>
      <h2>{t("privacy.aboutVisitorsHeading")}</h2>
      <p>{t("privacy.aboutVisitors")}</p>
      <p>{t("privacy.logs")}</p>
      <h2>{t("privacy.analyticsHeading")}</h2>
      <p>{t("privacy.analyticsBody")}</p>
      <h2>{t("privacy.aboutMpsHeading")}</h2>
      <p>{t("privacy.aboutMps")}</p>
      <p>{t("privacy.notPublishedLine")}</p>
      <ul>
        <li>{t("privacy.np1")}</li>
        <li>{t("privacy.np2")}</li>
        <li>{t("privacy.np3")}</li>
        <li>{t("privacy.np4")}</li>
      </ul>
      <h2>{t("privacy.rightHeading")}</h2>
      <p>
        {t("privacy.rightBody1")}
        <a href="/corrections" className="text-estonia hover:underline">{t("footer.corrections")}</a>
        {t("privacy.rightBody2")}
      </p>
      <h2>{t("privacy.photosHeading")}</h2>
      <p>{t("privacy.photosBody")}</p>
    </article>
  );
}

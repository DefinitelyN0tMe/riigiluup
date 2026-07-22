import { useTranslation } from "react-i18next";
import { CORRECTIONS_EMAIL } from "../../config";

export default function AboutPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("about.title")}</h1>
      <p>{t("about.intro")}</p>
      <h2>{t("about.editorial")}</h2>
      <p>{t("about.editorialBody")}</p>
      <h2>{t("about.whatPublic")}</h2>
      <ul>
        <li>{t("about.list.members")}</li>
        <li>{t("about.list.votes")}</li>
        <li>{t("about.list.bills")}</li>
        <li>{t("about.list.rates")}</li>
      </ul>
      <h2>{t("about.dataSource")}</h2>
      <p>
        {t("about.dataSourceBody")}
        <a href="https://api.riigikogu.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`api.riigikogu.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">
          api.riigikogu.ee
        </a>
        {t("about.dataSourceSuffix")}
      </p>
      <h2>{t("about.contact")}</h2>
      <p>
        {t("about.contactBody")}
        <a href={`mailto:${CORRECTIONS_EMAIL}`} className="text-estonia hover:underline">
          {CORRECTIONS_EMAIL}
        </a>
        .
      </p>
    </article>
  );
}

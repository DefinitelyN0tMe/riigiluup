import { useTranslation } from "react-i18next";

export default function SourcesPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("sources.title")}</h1>
      <p>{t("sources.intro")}</p>
      <h2>{t("sources.primary")}</h2>
      <p>
        {t("sources.primaryPre")}
        <a href="https://api.riigikogu.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`api.riigikogu.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">api.riigikogu.ee</a>
        {t("sources.primaryMid")}
        <a href="https://api.riigikogu.ee/v3/api-docs" target="_blank" rel="noopener noreferrer"
           aria-label={`Riigikogu OpenAPI (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">/v3/api-docs</a>
        {t("sources.primarySuffix")}
      </p>
      <h2>{t("sources.endpoints")}</h2>
      <ul>
        <li><code>/api/plenary-members</code>{t("sources.ep1a")}<code>/api/plenary-members/{`{uuid}`}</code>{t("sources.ep1b")}</li>
        <li><code>/api/usergroups</code>{t("sources.ep2")}</li>
        <li><code>/api/votings</code>{t("sources.ep3a")}<code>/api/votings/{`{uuid}`}</code>{t("sources.ep3b")}</li>
        <li><code>/api/volumes/drafts</code>{t("sources.ep4a")}<code>/api/volumes/drafts/{`{uuid}`}</code>{t("sources.ep4b")}</li>
        <li><code>/api/statistics/participations/member/{`{uuid}`}</code>{t("sources.ep5a")}<code>/api/statistics/votings/member/{`{uuid}`}</code>{t("sources.ep5b")}</li>
        <li><code>/api/files/{`{uuid}`}/download</code>{t("sources.ep6")}</li>
      </ul>
      <h2>{t("sources.license")}</h2>
      <p>
        {t("sources.licenseBody1")}
        <strong>{t("sources.licenseBody1b")}</strong>
        {t("sources.licenseBody1c")}
      </p>
      <h2>{t("sources.updateFrequency")}</h2>
      <p>{t("sources.updateBody")}</p>
    </article>
  );
}

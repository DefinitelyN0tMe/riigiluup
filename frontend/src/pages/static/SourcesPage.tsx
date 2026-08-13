import { useTranslation } from "react-i18next";

export default function SourcesPage() {
  const { t } = useTranslation();
  return (
    <article className="prose max-w-[820px] mx-auto px-5 sm:px-8 md:px-10 py-10 sm:py-14 text-slate-700">
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
        <li><code>/api/documents</code>{t("sources.ep7")}</li>
      </ul>
      <h2>{t("sources.license")}</h2>
      <p>
        {t("sources.licenseBody1")}
        <strong>{t("sources.licenseBody1b")}</strong>
        {t("sources.licenseBody1c")}
      </p>
      <h2>{t("sources.updateFrequency")}</h2>
      <p>{t("sources.updateBody")}</p>
      <h2>{t("sources.additional")}</h2>
      <h3>{t("sources.rahvaalgatus.title")}</h3>
      <p>
        {t("sources.rahvaalgatus.pre")}
        <a href="https://rahvaalgatus.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`Rahvaalgatus.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">Rahvaalgatus.ee</a>
        {t("sources.rahvaalgatus.mid")}
        {t("sources.rahvaalgatus.operator")}
        {t("sources.rahvaalgatus.suffix")}
      </p>
      <p>{t("sources.rahvaalgatus.what")}</p>
      <p>{t("sources.rahvaalgatus.cadence")}</p>

      <h3>{t("sources.wikidata.title")}</h3>
      <p>
        {t("sources.wikidata.pre")}
        <a href="https://www.wikidata.org" target="_blank" rel="noopener noreferrer"
           aria-label={`Wikidata (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">Wikidata</a>
        {t("sources.wikidata.suffix")} {t("sources.wikidata.cadence")}
      </p>

      <h3>{t("sources.riigiteataja.title")}</h3>
      <p>
        {t("sources.riigiteataja.pre")}
        <a href="https://www.riigiteataja.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`Riigi Teataja (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">Riigi Teataja</a>
        {t("sources.riigiteataja.suffix")}
      </p>

      <h3>{t("sources.elections.title")}</h3>
      <p>
        {t("sources.elections.pre")}
        <a href="https://www.valimised.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`valimised.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">valimised.ee</a>
        {t("sources.elections.suffix")}
      </p>

      <h3>{t("sources.molder.title")}</h3>
      <p>
        {t("sources.molder.pre")}
        <a href="https://www.eestipoliitika.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`eestipoliitika.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">eestipoliitika.ee</a>
        {t("sources.molder.suffix")}
      </p>

      <h3>{t("sources.partyfinance.title")}</h3>
      <p>
        {t("sources.partyfinance.pre")}
        <a href="https://www.erjk.ee" target="_blank" rel="noopener noreferrer"
           aria-label={`erjk.ee (${t("a11y.opensNewTab")})`}
           className="text-estonia hover:underline">erjk.ee</a>
        {t("sources.partyfinance.suffix")} {t("sources.partyfinance.cadence")}
      </p>

      <h2>{t("sources.notes.title")}</h2>
      <ul>
        <li>{t("sources.notes.interpellations")}</li>
        <li>{t("sources.notes.amendments")}</li>
        <li>{t("sources.notes.press")}</li>
        <li>{t("sources.notes.party")}</li>
      </ul>
    </article>
  );
}

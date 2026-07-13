export default function SourcesPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Data sources</h1>
      <p>
        All political data on Politico is imported from official Riigikogu open
        data. Nothing is scraped, editorialized, or purchased.
      </p>
      <h2>Primary source</h2>
      <p>
        Riigikogu Open Data API at{" "}
        <a href="https://api.riigikogu.ee" target="_blank" rel="noreferrer noopener"
           className="text-estonia hover:underline">api.riigikogu.ee</a>{" "}
        (OpenAPI spec at{" "}
        <a href="https://api.riigikogu.ee/v3/api-docs" target="_blank" rel="noreferrer noopener"
           className="text-estonia hover:underline">/v3/api-docs</a>).
      </p>
      <h2>Endpoints consumed</h2>
      <ul>
        <li><code>/api/plenary-members</code> and <code>/api/plenary-members/{`{uuid}`}</code> — MPs and their term details.</li>
        <li><code>/api/usergroups</code> — factions, committees, delegations, associations.</li>
        <li><code>/api/votings</code> and <code>/api/votings/{`{uuid}`}</code> — sittings, votes, per-MP decisions.</li>
        <li><code>/api/volumes/drafts</code> and <code>/api/volumes/drafts/{`{uuid}`}</code> — bills, stages, sponsors, topics.</li>
        <li><code>/api/statistics/participations/member/{`{uuid}`}</code> and <code>/api/statistics/votings/member/{`{uuid}`}</code> — pre-computed participation counts.</li>
        <li><code>/api/files/{`{uuid}`}/download</code> — MP photos, proxied through Politico for caching.</li>
      </ul>
      <h2>License</h2>
      <p>
        Riigikogu open data is released under <strong>Creative Commons Attribution-ShareAlike 3.0</strong>.
        This site's own aggregated content and code are released under the same license so downstream
        reuse is straightforward.
      </p>
      <h2>Update frequency</h2>
      <p>
        A daily job at 03:05 Europe/Tallinn refreshes members, groups, per-MP
        details, the last 7 days of votes, and the last 7 days of bills. The
        "Last sync" badge in the header shows the timestamp of the last
        successful members refresh.
      </p>
    </article>
  );
}

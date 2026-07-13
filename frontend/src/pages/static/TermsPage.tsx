export default function TermsPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Terms of use</h1>
      <h2>License</h2>
      <p>
        The aggregated data displayed on Politico is released under <strong>Creative Commons Attribution-ShareAlike 3.0</strong>,
        the same license used by the underlying Riigikogu open data. You may
        reuse it — including for commercial purposes — provided you (a) credit
        Politico and Riigikogu as sources, and (b) release derived data under
        the same license.
      </p>
      <p>The site's source code is available under an OSI-approved permissive open-source license.</p>

      <h2>No warranty</h2>
      <p>
        Politico is provided "as is". Every fact is sourced from a public API
        and can change without notice. The site aims for accuracy but cannot
        guarantee it. Use the "Source ↗" link on any card, chart, or metric to
        verify against the original before making decisions.
      </p>

      <h2>Editorial neutrality</h2>
      <p>
        Politico does not endorse parties, factions, or individual members. It
        does not display "best" or "worst" rankings, ideological scores, or
        sentiment analysis. Where a computed rate could be misread, the
        methodology pages describe the formula and its limits.
      </p>

      <h2>Fair use</h2>
      <p>
        Automated bulk scraping of Politico's public REST endpoints is
        allowed within reason (≤ 1 req/s per IP, same as the upstream
        Riigikogu limit). For heavy programmatic use, prefer the upstream
        Riigikogu API directly.
      </p>
    </article>
  );
}

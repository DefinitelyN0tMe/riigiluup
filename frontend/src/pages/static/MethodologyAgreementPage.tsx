export default function MethodologyAgreementPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Pairwise agreement methodology</h1>

      <p>
        Pairwise agreement measures how often two MPs cast the same comparable
        choice on votes where both actually voted.
      </p>

      <h2>Formula</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        rate = same / (same + different)
      </p>

      <h2>What counts as "same" or "different"</h2>
      <ul>
        <li><strong>Same:</strong> both MPs cast the same comparable choice (both FOR, both AGAINST, or both ABSTAINED).</li>
        <li><strong>Different:</strong> both cast comparable choices but they differ.</li>
      </ul>

      <h2>What is reported separately, NOT in the rate</h2>
      <ul>
        <li>At least one MP did not participate (did-not-vote, absent, attendance-check presence).</li>
      </ul>

      <h2>Interpretation</h2>
      <p>
        A high agreement rate says the two MPs, when both voting, made the same
        choice most of the time. It does not measure ideological similarity in
        general; it only measures overlap on this specific set of votes in this
        specific time window.
      </p>
    </article>
  );
}

export default function MethodologyAlignmentPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Group alignment methodology</h1>

      <p>
        Group alignment measures how often an MP's vote matches their
        parliamentary group's majority position. It is a factual comparison,
        not a judgment about loyalty or independence.
      </p>

      <h2>Formula</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        rate = matches / eligible votes
      </p>

      <h2>What counts as "eligible"</h2>
      <ul>
        <li>The MP cast a comparable choice (FOR, AGAINST, or ABSTAINED).</li>
        <li>The MP's group cast at least one comparable choice.</li>
        <li>The group had a <em>clear</em> majority — a single choice strictly outnumbered the others. A tie is NOT a clear majority.</li>
      </ul>

      <h2>What counts as a "match"</h2>
      <p>Among eligible votes, the MP's choice equals the group's majority choice.</p>

      <h2>What is intentionally excluded from the rate</h2>
      <ul>
        <li>Did-not-vote and absent — displayed separately as "recent deviations" and via voting-participation.</li>
        <li>Votes where the group split evenly (no clear majority).</li>
        <li>Attendance checks and secret votes.</li>
      </ul>

      <h2>Interpretation</h2>
      <p>
        A high group alignment says the MP voted the same way as most of their
        group when a majority position existed. It does not say the MP agrees
        with every group position, and it does not say the group's position is
        correct or incorrect. A low alignment simply means the MP diverged on
        some votes with a clear group majority; the deviations list shows which.
      </p>
    </article>
  );
}

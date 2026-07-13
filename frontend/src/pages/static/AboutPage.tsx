export default function AboutPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">About Politico</h1>
      <p>
        Politico is a public civic-tech tool that makes the work of the Estonian
        Riigikogu easier to understand. It aggregates the parliament's own open
        data — MPs, votes, bills — and presents it as neutral profiles,
        timelines, and comparisons. Every fact links back to the official
        source.
      </p>
      <h2>Editorial stance</h2>
      <p>
        The service does not assign ideological labels, moral scores, or
        rankings. It reports factual counts and rates. Where a metric could be
        misinterpreted, we document the formula, what is included, what is
        excluded, and the known limitations.
      </p>
      <h2>What is public here</h2>
      <ul>
        <li>Member profiles, faction, committees, electoral district.</li>
        <li>Roll-call votes and attendance checks.</li>
        <li>Bills (eelnõu), their stages, sponsors, and Eurovoc topic tags.</li>
        <li>Statistical rates (participation, alignment, pairwise agreement).</li>
      </ul>
      <h2>Data source</h2>
      <p>
        Riigikogu Open Data API at{" "}
        <a href="https://api.riigikogu.ee" target="_blank" rel="noreferrer noopener"
           className="text-estonia hover:underline">
          api.riigikogu.ee
        </a>{" "}
        under CC BY-SA 3.0.
      </p>
      <h2>Contact</h2>
      <p>
        Errors, corrections, questions:{" "}
        <a href="mailto:corrections@politico.example" className="text-estonia hover:underline">
          corrections@politico.example
        </a>
        .
      </p>
    </article>
  );
}

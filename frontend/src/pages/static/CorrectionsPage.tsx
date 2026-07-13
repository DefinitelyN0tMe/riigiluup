import { CORRECTIONS_EMAIL } from "../../config";

export default function CorrectionsPage() {
  const subject = encodeURIComponent("Politico correction");
  const body = encodeURIComponent(
      "Which record is wrong? (URL or ID)\n\n" +
      "What is the correction?\n\n" +
      "Where can we verify (Riigikogu link or other official source)?\n\n" +
      "Your name (optional):\n"
  );
  const mailto = `mailto:${CORRECTIONS_EMAIL}?subject=${subject}&body=${body}`;
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Corrections</h1>
      <p>
        Politico mirrors Riigikogu's open data — most factual errors originate
        upstream and flow into Politico on the next daily refresh once fixed at
        the source. If you spot an issue that Politico introduces (a broken
        chart, a wrong slug, a mislabeled metric), please tell us directly.
      </p>
      <h2>Send a correction</h2>
      <p>
        Email <a href={mailto} className="text-estonia hover:underline">{CORRECTIONS_EMAIL}</a> with:
      </p>
      <ol>
        <li>The URL or ID of the record you are reporting.</li>
        <li>What is currently shown and what should be shown instead.</li>
        <li>A source link for verification.</li>
      </ol>
      <p>
        The <em>Send a correction</em> button below opens your mail client with
        that template pre-filled.
      </p>
      <p>
        <a
          href={mailto}
          className="inline-flex items-center px-4 py-2 rounded-md bg-estonia text-white hover:bg-blue-700 no-underline"
        >
          Send a correction
        </a>
      </p>
      <h2>What we do</h2>
      <p>
        Every credible report is reviewed by a maintainer within 7 days. If the
        issue is with Politico's own presentation, it is patched and released.
        If the issue is with the upstream Riigikogu data, we forward it to
        Riigikogu's data team and note the report on this page.
      </p>
      <h2>What we do not do</h2>
      <ul>
        <li>Retract public voting records — a vote is a public fact.</li>
        <li>Re-label MPs or parties on request.</li>
        <li>Assign or remove "activity scores" — the site does not compute them.</li>
      </ul>
    </article>
  );
}

export default function PrivacyPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Privacy</h1>
      <h2>What Politico stores about visitors</h2>
      <p>
        Nothing linked to your identity. There is no login, no cookies for
        tracking, no analytics platform, no advertising.
      </p>
      <p>
        The Nginx reverse proxy writes standard HTTP access logs (IP address,
        request URL, user-agent, timestamp) for operational reasons. Logs
        are retained for up to 30 days and then rotated out.
      </p>
      <h2>What Politico publishes about MPs</h2>
      <p>
        Only what is already a matter of public record via the Riigikogu:
        name, party, faction, electoral district, committee memberships, votes,
        speeches count, and the biography maintained by the MP or the Riigikogu
        chancellery.
      </p>
      <p>Not published:</p>
      <ul>
        <li>Home address, personal phone, private email.</li>
        <li>Family members.</li>
        <li>Health information.</li>
        <li>Social-media posts (only counts of official press mentions where reported by Riigikogu).</li>
      </ul>
      <h2>Right to correction</h2>
      <p>
        See <a href="/corrections" className="text-estonia hover:underline">Corrections</a>. Every
        record links to its official source; corrections at the source flow into
        Politico on the next daily refresh.
      </p>
      <h2>Photos</h2>
      <p>
        MP photos are cached from the Riigikogu file endpoint to reduce load on
        their servers. They are the property of the Riigikogu and are used
        under the CC BY-SA 3.0 license.
      </p>
    </article>
  );
}

export default function MethodologyParticipationPage() {
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">Participation methodology</h1>

      <h2>Voting participation</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        rate = participated / (participated + present + absent + neutral)
      </p>
      <p>
        <strong>Participated</strong> counts every roll-call vote where the MP
        cast FOR, AGAINST, or ABSTAINED.
      </p>
      <p>Excluded from the numerator:</p>
      <ul>
        <li>Did-not-vote (present but did not press a button).</li>
        <li>Absent from the sitting.</li>
        <li>Neutral (a rare procedural status used by the Riigikogu API).</li>
      </ul>
      <p>Excluded from BOTH numerator and denominator:</p>
      <ul>
        <li>Attendance-check "votes" (they are not roll-call votes).</li>
        <li>Secret votes (no per-MP records are published).</li>
        <li>Votes where the MP did not have an active mandate.</li>
      </ul>
      <p>
        Source: Riigikogu API endpoint{" "}
        <code>/api/statistics/votings/member/{`{uuid}`}?startDate=…&endDate=…</code>.
      </p>

      <h2>Attendance-check presence</h2>
      <p className="font-mono text-sm bg-slate-50 p-3 border border-slate-200 rounded">
        rate = attended / total sittings with an attendance check
      </p>
      <p>
        Attendance checks are a specific procedure in the Riigikogu where
        members register presence at a sitting. <strong>This is NOT total working
        hours or a full-day attendance measure.</strong>
      </p>
      <p>
        A high attendance-check rate does not mean an MP is present for the
        entire sitting or every committee. A low rate does not mean the MP was
        absent from all work — they may have been in committee meetings,
        official trips, or on leave.
      </p>
      <p>
        Source: Riigikogu API endpoint{" "}
        <code>/api/statistics/participations/member/{`{uuid}`}?startDate=…&endDate=…</code>.
      </p>

      <h2>Time period</h2>
      <p>
        Metrics on MP profiles default to the current parliamentary term —
        the 15th Riigikogu, starting 2023-04-10. When Riigikogu reports "0 of 0",
        the rate is displayed as an em dash ("—") rather than 0%.
      </p>
    </article>
  );
}

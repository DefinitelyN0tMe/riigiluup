import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchVoteDetail } from "../api/votes";
import VoteResultBar from "../components/VoteResultBar";

const CHOICE_LABEL: Record<string, string> = {
  FOR: "For",
  AGAINST: "Against",
  ABSTAINED: "Abstained",
  DID_NOT_VOTE: "Did not vote",
  ABSENT: "Absent",
  PRESENT: "Present",
  UNKNOWN: "Unknown",
};

const CHOICE_CLASS: Record<string, string> = {
  FOR: "text-estonia",
  AGAINST: "text-orange-600",
  ABSTAINED: "text-slate-600",
  DID_NOT_VOTE: "text-slate-500",
  ABSENT: "text-slate-400",
  PRESENT: "text-estonia",
  UNKNOWN: "text-slate-400",
};

export default function VoteDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["vote", id],
    queryFn: () => fetchVoteDetail(id!),
    enabled: !!id,
  });

  if (isLoading) return <p className="text-slate-500">Loading…</p>;
  if (error) return <p className="text-red-600">Failed to load. {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500">Not found.</p>;

  const when = data.startedAt ? new Date(data.startedAt).toLocaleString() : "—";

  return (
    <div className="space-y-6">
      <div>
        <Link to="/votes" className="text-sm text-estonia hover:underline">← All votes</Link>
      </div>

      <header className="space-y-1">
        <h1 className="text-2xl font-semibold text-ink">{data.description ?? "(no description)"}</h1>
        <p className="text-sm text-slate-600">
          {data.type} · #{data.votingNumber ?? "—"} · {when}
        </p>
        {data.sittingTitle && (
          <p className="text-sm text-slate-500">Sitting: {data.sittingTitle}</p>
        )}
        <a
          href={data.sourceUrl} target="_blank" rel="noreferrer noopener"
          className="text-xs text-estonia hover:underline inline-block mt-1"
        >Riigikogu source ↗</a>
      </header>

      <section aria-label="Result">
        <VoteResultBar
          inFavor={data.resultInFavor}
          against={data.resultAgainst}
          abstained={data.resultAbstained}
          didNotVote={data.resultPresent}
          absent={data.resultAbsent}
        />
      </section>

      <section aria-label="Faction breakdown">
        <h2 className="text-lg font-semibold text-ink mb-2">Breakdown by faction</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {data.factionBreakdowns.map((f) => (
            <div key={f.factionExternalId ?? f.factionName}
                 className="border border-slate-200 rounded-lg p-3">
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-medium truncate">{f.factionName}</span>
                <span className="text-xs text-slate-500 shrink-0">{f.total} MPs</span>
              </div>
              <VoteResultBar
                inFavor={f.inFavor}
                against={f.against}
                abstained={f.abstained}
                didNotVote={f.didNotVote + f.present}
                absent={f.absent}
              />
            </div>
          ))}
        </div>
      </section>

      <section aria-label="Individual votes">
        <h2 className="text-lg font-semibold text-ink mb-2">
          Individual votes ({data.individualVotes.length})
        </h2>
        <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
          {data.individualVotes.map((iv) => (
            <li key={iv.memberExternalId ?? iv.memberFullName}
                className="flex justify-between items-center p-3 text-sm">
              <span className="min-w-0">
                {iv.memberSlug ? (
                  <Link to={`/politicians/${iv.memberSlug}`} className="hover:underline text-ink">
                    {iv.memberFullName}
                  </Link>
                ) : iv.memberFullName}
                <span className="text-slate-500"> — {iv.factionName ?? "Unaffiliated"}</span>
              </span>
              <span className={`shrink-0 font-medium ${CHOICE_CLASS[iv.choice] ?? ""}`}>
                {CHOICE_LABEL[iv.choice] ?? iv.choice}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

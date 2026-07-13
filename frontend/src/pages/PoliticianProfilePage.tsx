import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchProfile } from "../api/politicians";
import { resolveMediaUrl } from "../api/client";
import { fetchPoliticianVotes } from "../api/votes";
import { fetchPoliticianLegislation } from "../api/legislation";
import type { PoliticianVote } from "../types";
import MetricCard from "../components/MetricCard";
import CommitteeChip from "../components/CommitteeChip";
import FactionBadge from "../components/FactionBadge";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

const CHOICE_LABEL_SHORT: Record<PoliticianVote["choice"], string> = {
  FOR: "For",
  AGAINST: "Against",
  ABSTAINED: "Abstained",
  DID_NOT_VOTE: "Did not vote",
  ABSENT: "Absent",
  PRESENT: "Present",
  UNKNOWN: "Unknown",
};

function VotingHistory({ slug }: { slug: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ["politician-votes", slug],
    queryFn: () => fetchPoliticianVotes(slug, 0, 20),
  });
  if (isLoading) return <p className="text-sm text-slate-500">Loading voting history…</p>;
  if (!data || data.items.length === 0)
    return <p className="text-sm text-slate-500">No recorded votes yet.</p>;
  return (
    <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
      {data.items.map((v) => {
        const when = v.startedAt ? new Date(v.startedAt).toLocaleDateString() : "";
        return (
          <li key={v.voteEventId} className="flex justify-between items-center p-3 text-sm gap-3">
            <span className="min-w-0">
              <Link to={`/votes/${v.voteEventId}`} className="hover:underline text-ink">
                {v.description ?? "(no description)"}
              </Link>
              <span className="text-slate-500 block text-xs">{when}</span>
            </span>
            <span className="shrink-0 font-medium">
              {CHOICE_LABEL_SHORT[v.choice] ?? v.choice}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function BillsSponsored({ slug }: { slug: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ["politician-legislation", slug],
    queryFn: () => fetchPoliticianLegislation(slug, 0, 10),
  });
  if (isLoading) return <p className="text-sm text-slate-500">Loading bills…</p>;
  if (!data || data.totalSponsored === 0)
    return <p className="text-sm text-slate-500">No bills sponsored.</p>;
  return (
    <div>
      <p className="text-3xl font-semibold text-ink">{data.totalSponsored}</p>
      <p className="text-xs text-slate-500 mb-2">bills sponsored / initiated</p>
      <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
        {data.items.items.map((i) => (
          <li key={i.id} className="p-3 text-sm">
            <a href={`/legislation/${i.id}`} className="hover:underline text-ink">
              {i.mark != null && <span className="text-slate-400 mr-2">#{i.mark}</span>}
              {i.title}
            </a>
            <span className="text-slate-500 block text-xs mt-0.5">
              {i.phase} · Initiated {i.initiatedDate ?? "—"}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function PoliticianProfilePage() {
  const { slug } = useParams<{ slug: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["profile", slug],
    queryFn: () => fetchProfile(slug!),
    enabled: !!slug,
  });

  if (isLoading) return <p className="text-slate-500">Loading…</p>;
  if (error) return <p className="text-red-600">Failed to load. {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500">Not found.</p>;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/politicians" className="text-sm text-estonia hover:underline">
          ← All MPs
        </Link>
      </div>

      <header className="flex items-start gap-4">
        {resolveMediaUrl(data.photoUrl) ? (
          <img
            src={resolveMediaUrl(data.photoUrl)}
            alt=""
            className="w-24 h-24 rounded-full object-cover bg-slate-100"
          />
        ) : (
          <div className="w-24 h-24 rounded-full bg-slate-100" />
        )}
        <div className="min-w-0">
          <h1 className="text-3xl font-semibold text-ink">{data.fullName}</h1>
          <div className="mt-1"><FactionBadge faction={data.faction} party={data.party} /></div>
          {data.electoralDistrict && (
            <div className="text-sm text-slate-600 mt-1">
              Electoral district: {data.electoralDistrict}
            </div>
          )}
          <a
            href={data.officialProfileUrl ?? undefined}
            target="_blank"
            rel="noreferrer noopener"
            className="text-sm text-estonia hover:underline mt-1 inline-block"
          >
            Official profile ↗
          </a>
        </div>
      </header>

      <section aria-label="Metrics" className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <MetricCard
          label="Voting participation"
          value={pct(data.voting.participationRate)}
          hint={`${data.voting.participated} of ${data.voting.totalVotings} recorded votes`}
          sourceUrl={data.voting.sourceUrl}
        />
        <MetricCard
          label="Attendance-check presence"
          value={pct(data.participation.participationRate)}
          hint={`${data.participation.attended} of ${data.participation.totalSittings} attendance checks`}
          sourceUrl={data.participation.sourceUrl}
        />
        <MetricCard
          label="Parliament seniority"
          value={data.parliamentSeniorityDays == null
              ? "—"
              : `${Math.floor(data.parliamentSeniorityDays / 365)} years`}
          hint="Days of parliamentary service reported by Riigikogu"
          sourceUrl={data.sourceUrl}
        />
      </section>

      {data.groupAlignment && (
        <section aria-label="Group alignment">
          <h2 className="text-lg font-semibold text-ink mb-2">Group alignment</h2>
          <div className="border border-slate-200 rounded-lg p-4">
            <p className="text-3xl font-semibold text-ink">
              {data.groupAlignment.rate == null ? "—" : `${(data.groupAlignment.rate * 100).toFixed(1)}%`}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              {data.groupAlignment.matches} of {data.groupAlignment.eligible} eligible votes matched the faction majority.
            </p>
            <p className="text-xs text-slate-500 mt-2">{data.groupAlignment.methodologyNote}</p>
            {data.groupAlignment.recentDeviations.length > 0 && (
              <div className="mt-3">
                <h3 className="text-sm font-medium text-ink mb-1">Recent deviations</h3>
                <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
                  {data.groupAlignment.recentDeviations.map((d) => {
                    const when = d.startedAt ? new Date(d.startedAt).toLocaleDateString() : "";
                    return (
                      <li key={d.voteEventId} className="p-3 text-sm flex justify-between items-start gap-3">
                        <span className="min-w-0">
                          <a href={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
                            {d.voteEventDescription ?? "(no description)"}
                          </a>
                          <span className="text-slate-500 block text-xs">{when}</span>
                        </span>
                        <span className="shrink-0 text-xs text-slate-500 text-right">
                          MP: <span className="font-medium text-ink">{d.memberChoice ?? "—"}</span>
                          <br />
                          Faction: <span className="font-medium text-ink">{d.factionMajorityChoice ?? "—"}</span>
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}
          </div>
        </section>
      )}

      {data.slug && (
        <section aria-label="Bills sponsored">
          <h2 className="text-lg font-semibold text-ink mb-2">Bills sponsored</h2>
          <div className="border border-slate-200 rounded-lg p-4">
            <BillsSponsored slug={data.slug} />
          </div>
        </section>
      )}

      {data.committees.length > 0 && (
        <section aria-label="Committees">
          <h2 className="text-lg font-semibold text-ink mb-2">Committees</h2>
          <ul className="flex flex-wrap gap-2">
            {data.committees.map((c) => (
              <CommitteeChip key={`${c.name}-${c.role}`} c={c} />
            ))}
          </ul>
        </section>
      )}

      {data.slug && (
        <section aria-label="Recent votes">
          <h2 className="text-lg font-semibold text-ink mb-2">Recent votes</h2>
          <VotingHistory slug={data.slug} />
        </section>
      )}

      {data.biographyHtml && (
        <section aria-label="Biography">
          <h2 className="text-lg font-semibold text-ink mb-2">Biography</h2>
          <div
            className="prose max-w-none text-slate-700"
            dangerouslySetInnerHTML={{ __html: data.biographyHtml }}
          />
        </section>
      )}
    </div>
  );
}

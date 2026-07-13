import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchProfile } from "../api/politicians";
import MetricCard from "../components/MetricCard";
import CommitteeChip from "../components/CommitteeChip";
import FactionBadge from "../components/FactionBadge";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
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
        {data.photoUrl ? (
          <img
            src={data.photoUrl}
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

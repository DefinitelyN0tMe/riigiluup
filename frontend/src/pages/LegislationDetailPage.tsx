import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchLegislationDetail } from "../api/legislation";
import StageTimeline from "../components/StageTimeline";
import TopicChip from "../components/TopicChip";

const PHASE_LABEL: Record<string, string> = {
  SUBMITTED: "Submitted",
  IN_COMMITTEE: "In committee",
  IN_READINGS: "In readings",
  ADOPTED: "Adopted",
  REJECTED: "Rejected",
  WITHDRAWN: "Withdrawn",
  OTHER: "Other",
};

const SPONSOR_KIND_LABEL: Record<string, string> = {
  PLENARY_MEMBER: "MP",
  FACTION: "Faction",
  COMMITTEE: "Committee",
  ORGAN: "Organ",
  OTHER: "Other",
};

export default function LegislationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["legislation", id],
    queryFn: () => fetchLegislationDetail(id!),
    enabled: !!id,
  });

  if (isLoading) return <p className="text-slate-500">Loading…</p>;
  if (error) return <p className="text-red-600">Failed to load. {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500">Not found.</p>;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/legislation" className="text-sm text-estonia hover:underline">← All bills</Link>
      </div>

      <header className="space-y-1">
        <h1 className="text-2xl font-semibold text-ink">
          {data.mark != null && <span className="text-slate-400 mr-2">#{data.mark}</span>}
          {data.title}
        </h1>
        <p className="text-sm text-slate-600">
          {data.draftTypeCode ? `${data.draftTypeCode} · ` : ""}
          {PHASE_LABEL[data.phase] ?? data.phase}
          {data.initiatedDate ? ` · Initiated ${new Date(data.initiatedDate).toLocaleDateString()}` : ""}
          {data.acceptedDate ? ` · Accepted ${new Date(data.acceptedDate).toLocaleDateString()}` : ""}
        </p>
        {data.leadingCommitteeName && (
          <p className="text-sm text-slate-500">Leading committee: {data.leadingCommitteeName}</p>
        )}
        <a href={data.sourceUrl} target="_blank" rel="noreferrer noopener"
           className="text-xs text-estonia hover:underline inline-block mt-1">
          Riigikogu source ↗
        </a>
      </header>

      {data.topics.length > 0 && (
        <section aria-label="Topics">
          <div className="flex flex-wrap">
            {data.topics.map((t) => <TopicChip key={t.edid} t={t} />)}
          </div>
        </section>
      )}

      {data.introduction && (
        <section aria-label="Introduction">
          <h2 className="text-lg font-semibold text-ink mb-2">Introduction</h2>
          <p className="text-sm text-slate-700 whitespace-pre-line">{data.introduction}</p>
        </section>
      )}

      <section aria-label="Sponsors">
        <h2 className="text-lg font-semibold text-ink mb-2">Sponsors ({data.sponsors.length})</h2>
        <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
          {data.sponsors.map((s, idx) => (
            <li key={s.externalId ?? `${idx}`} className="p-3 text-sm flex justify-between gap-3">
              <span className="min-w-0">
                {s.memberSlug ? (
                  <Link to={`/politicians/${s.memberSlug}`} className="hover:underline text-ink">
                    {s.memberFullName ?? s.displayName}
                  </Link>
                ) : (s.displayName ?? "(no name)")}
              </span>
              <span className="shrink-0 text-xs text-slate-500">
                {SPONSOR_KIND_LABEL[s.kind] ?? s.kind}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Timeline">
        <h2 className="text-lg font-semibold text-ink mb-2">Legislative timeline</h2>
        <StageTimeline stages={data.stages} />
      </section>
    </div>
  );
}

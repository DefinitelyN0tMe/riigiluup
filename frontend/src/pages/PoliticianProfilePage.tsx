import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchProfile } from "../api/politicians";
import { resolveMediaUrl } from "../api/client";
import { fetchPoliticianVotes } from "../api/votes";
import { fetchPoliticianLegislation } from "../api/legislation";
import MetricCard from "../components/MetricCard";
import CommitteeChip from "../components/CommitteeChip";
import FactionBadge from "../components/FactionBadge";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

function VotingHistory({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["politician-votes", slug],
    queryFn: () => fetchPoliticianVotes(slug, 0, 20),
  });
  if (isLoading) return <p className="text-sm text-slate-500">{t("profile.loadingVotes")}</p>;
  if (!data || data.items.length === 0)
    return <p className="text-sm text-slate-500">{t("profile.noVotes")}</p>;
  return (
    <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
      {data.items.map((v) => {
        const when = v.startedAt ? new Date(v.startedAt).toLocaleDateString() : "";
        return (
          <li key={v.voteEventId} className="flex justify-between items-center p-3 text-sm gap-3">
            <span className="min-w-0">
              <Link to={`/votes/${v.voteEventId}`} className="hover:underline text-ink">
                {v.description ?? t("common.noDescription")}
              </Link>
              <span className="text-slate-500 block text-xs">{when}</span>
            </span>
            <span className="shrink-0 font-medium">
              {t(`choice.${v.choice}` as const, { defaultValue: v.choice })}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function BillsSponsored({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["politician-legislation", slug],
    queryFn: () => fetchPoliticianLegislation(slug, 0, 10),
  });
  if (isLoading) return <p className="text-sm text-slate-500">{t("profile.loadingBills")}</p>;
  if (!data || data.totalSponsored === 0)
    return <p className="text-sm text-slate-500">{t("profile.noBills")}</p>;
  return (
    <div>
      <p className="text-3xl font-semibold text-ink">{data.totalSponsored}</p>
      <p className="text-xs text-slate-500 mb-2">{t("profile.billsSponsoredCaption")}</p>
      <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
        {data.items.items.map((i) => (
          <li key={i.id} className="p-3 text-sm">
            <a href={`/legislation/${i.id}`} className="hover:underline text-ink">
              {i.mark != null && <span className="text-slate-400 mr-2">#{i.mark}</span>}
              {i.title}
            </a>
            <span className="text-slate-500 block text-xs mt-0.5">
              {t("profile.initiatedLine", { phase: t(`phase.${i.phase}` as const, { defaultValue: i.phase }), date: i.initiatedDate ?? "—" })}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function PoliticianProfilePage() {
  const { t } = useTranslation();
  const { slug } = useParams<{ slug: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["profile", slug],
    queryFn: () => fetchProfile(slug!),
    enabled: !!slug,
  });

  if (isLoading) return <p className="text-slate-500" role="status">{t("common.loading")}</p>;
  if (error) return <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500" role="status">{t("common.notFound")}</p>;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/politicians" className="text-sm text-estonia hover:underline">
          {t("politicians.backAll")}
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
              {t("profile.electoralDistrict", { name: data.electoralDistrict })}
            </div>
          )}
          <a
            href={data.officialProfileUrl ?? undefined}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Official profile on Riigikogu (opens in new tab)"
            className="text-sm text-estonia hover:underline mt-1 inline-block"
          >
            {t("common.officialProfile")}
          </a>
        </div>
      </header>

      <section aria-label="Metrics" className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <MetricCard
          label={t("profile.metrics.votingParticipation")}
          value={pct(data.voting.participationRate)}
          hint={t("profile.metrics.votingHint", { participated: data.voting.participated, total: data.voting.totalVotings })}
          sourceUrl={data.voting.sourceUrl}
        />
        <MetricCard
          label={t("profile.metrics.attendancePresence")}
          value={pct(data.participation.participationRate)}
          hint={t("profile.metrics.attendanceHint", { attended: data.participation.attended, total: data.participation.totalSittings })}
          sourceUrl={data.participation.sourceUrl}
        />
        <MetricCard
          label={t("profile.metrics.seniority")}
          value={data.parliamentSeniorityDays == null
              ? "—"
              : t("profile.metrics.seniorityYears", { years: Math.floor(data.parliamentSeniorityDays / 365) })}
          hint={t("profile.metrics.seniorityHint")}
          sourceUrl={data.sourceUrl}
        />
      </section>

      {data.groupAlignment && (
        <section aria-label="Group alignment">
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.groupAlignment")}</h2>
          <div className="border border-slate-200 rounded-lg p-4">
            <p className="text-3xl font-semibold text-ink">
              {data.groupAlignment.rate == null ? "—" : `${(data.groupAlignment.rate * 100).toFixed(1)}%`}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              {t("profile.groupAlignmentDetail", { matches: data.groupAlignment.matches, eligible: data.groupAlignment.eligible })}
            </p>
            <p className="text-xs text-slate-500 mt-2">{data.groupAlignment.methodologyNote}</p>
            {data.groupAlignment.recentDeviations.length > 0 && (
              <div className="mt-3">
                <h3 className="text-sm font-medium text-ink mb-1">{t("profile.recentDeviations")}</h3>
                <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
                  {data.groupAlignment.recentDeviations.map((d) => {
                    const when = d.startedAt ? new Date(d.startedAt).toLocaleDateString() : "";
                    return (
                      <li key={d.voteEventId} className="p-3 text-sm flex justify-between items-start gap-3">
                        <span className="min-w-0">
                          <a href={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
                            {d.voteEventDescription ?? t("common.noDescription")}
                          </a>
                          <span className="text-slate-500 block text-xs">{when}</span>
                        </span>
                        <span className="shrink-0 text-xs text-slate-500 text-right">
                          {t("profile.mpChoice")}: <span className="font-medium text-ink">{d.memberChoice ?? "—"}</span>
                          <br />
                          {t("profile.factionChoice")}: <span className="font-medium text-ink">{d.factionMajorityChoice ?? "—"}</span>
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
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.recentBills")}</h2>
          <div className="border border-slate-200 rounded-lg p-4">
            <BillsSponsored slug={data.slug} />
          </div>
        </section>
      )}

      {data.committees.length > 0 && (
        <section aria-label="Committees">
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.committees")}</h2>
          <ul className="flex flex-wrap gap-2">
            {data.committees.map((c) => (
              <CommitteeChip key={`${c.name}-${c.role}`} c={c} />
            ))}
          </ul>
        </section>
      )}

      {data.slug && (
        <section aria-label="Recent votes">
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.recentVotes")}</h2>
          <VotingHistory slug={data.slug} />
        </section>
      )}

      {data.biographyHtml && (
        <section aria-label="Biography">
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.biography")}</h2>
          <div
            className="prose max-w-none text-slate-700"
            dangerouslySetInnerHTML={{ __html: data.biographyHtml }}
          />
        </section>
      )}
    </div>
  );
}

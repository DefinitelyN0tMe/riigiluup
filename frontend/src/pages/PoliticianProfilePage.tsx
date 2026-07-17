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
import MpTopicRadar from "../components/analytics/MpTopicRadar";
import AffiliationTimeline from "../components/analytics/AffiliationTimeline";
import DeviationsCalendar from "../components/analytics/DeviationsCalendar";
import SimilarPeers from "../components/analytics/SimilarPeers";
import { fetchMpTopicRadar, fetchMpDeviationsTimeline, fetchMpSimilarPeers } from "../api/analytics";
import { formatDate } from "../lib/formatDate";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

function ActivityStat({ value, label, hint }: { value: number; label: string; hint?: string }) {
  const { i18n } = useTranslation();
  return (
    <div>
      <div className="font-display font-bold text-[28px] leading-none tracking-[-0.03em] text-ink">
        {value.toLocaleString(i18n.resolvedLanguage)}
      </div>
      <div
        className={`font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1.5${
          hint ? " border-b border-dotted border-muted/60 cursor-help inline-block" : ""
        }`}
        title={hint}
      >
        {label}
      </div>
    </div>
  );
}

function LoadFailed() {
  const { t } = useTranslation();
  return <p className="text-sm text-hot-deep" role="alert">{t("profile.loadFailed")}</p>;
}

function VotingHistory({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["politician-votes", slug],
    queryFn: () => fetchPoliticianVotes(slug, 0, 20),
  });
  if (isLoading) return <p className="text-sm text-slate-500">{t("profile.loadingVotes")}</p>;
  if (error) return <LoadFailed />;
  if (!data || data.items.length === 0)
    return <p className="text-sm text-slate-500">{t("profile.noVotes")}</p>;
  return (
    <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
      {data.items.map((v) => {
        const when = v.startedAt ? formatDate(v.startedAt) : "";
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

function MpTopicRadarSection({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["mp-topic-radar", slug],
    queryFn: () => fetchMpTopicRadar(slug, 8),
  });
  if (isLoading) return <p className="text-sm text-muted font-mono tracking-[0.06em]">{t("viz.loading")}</p>;
  if (error) return <LoadFailed />;
  if (!data) return null;
  return <MpTopicRadar data={data} />;
}

function DeviationsCalendarSection({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["mp-deviations-timeline", slug],
    queryFn: () => fetchMpDeviationsTimeline(slug, 12),
  });
  if (isLoading) return <p className="text-sm text-muted font-mono tracking-[0.06em]">{t("viz.loading")}</p>;
  if (error) return <LoadFailed />;
  if (!data) return null;
  return <DeviationsCalendar data={data} />;
}

function SimilarPeersSection({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["mp-similar-peers", slug],
    queryFn: () => fetchMpSimilarPeers(slug, 5),
  });
  if (isLoading) return <p className="text-sm text-muted font-mono tracking-[0.06em]">{t("viz.loading")}</p>;
  if (error) return <LoadFailed />;
  if (!data) return null;
  return <SimilarPeers data={data} currentSlug={slug} />;
}

function BillsSponsored({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["politician-legislation", slug],
    queryFn: () => fetchPoliticianLegislation(slug, 0, 10),
  });
  if (isLoading) return <p className="text-sm text-slate-500">{t("profile.loadingBills")}</p>;
  if (error) return <LoadFailed />;
  if (!data || data.totalSponsored === 0)
    return <p className="text-sm text-slate-500">{t("profile.noBills")}</p>;
  return (
    <div>
      <p className="text-3xl font-semibold text-ink">{data.totalSponsored}</p>
      <p className="text-xs text-slate-500 mb-2">{t("profile.billsSponsoredCaption")}</p>
      <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
        {data.items.items.map((i) => (
          <li key={i.id} className="p-3 text-sm">
            <Link to={`/legislation/${i.id}`} className="hover:underline text-ink">
              {i.mark != null && <span className="text-slate-400 mr-2">#{i.mark}</span>}
              {i.title}
            </Link>
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
  const { t, i18n } = useTranslation();
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
    <div className="space-y-6 max-w-[1200px] mx-auto w-full px-5 sm:px-8 md:px-10 py-8 sm:py-12">
      <div>
        <Link to="/politicians" className="text-sm text-blue hover:underline font-mono tracking-[0.06em]">
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
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="text-3xl font-semibold text-ink">{data.fullName}</h1>
            {!data.active && (
              <span
                className="inline-flex items-center px-2 py-0.5 rounded-full bg-slate-200 text-slate-700 text-[11px] font-mono tracking-[0.14em] uppercase"
                title={t("profile.formerMpTooltip", "This person is no longer a member of the current Riigikogu. Their historical votes and bills are still shown.")}
              >
                {t("profile.formerMp", "Endine saadik")}
              </span>
            )}
          </div>
          <div className="mt-1"><FactionBadge faction={data.faction} party={data.party} /></div>
          {data.electoralDistrict && (
            <div className="text-sm text-slate-600 mt-1">
              {t("profile.electoralDistrict", { name: data.electoralDistrict })}
            </div>
          )}
          <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm">
            <a
              href={data.officialProfileUrl ?? undefined}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${t("common.officialProfile").replace(" ↗", "")} (${t("a11y.opensNewTab")})`}
              className="text-estonia hover:underline"
            >
              {t("common.officialProfile")}
            </a>
            <a
              href={data.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${t("common.apiData").replace(" ↗", "")} (${t("a11y.opensNewTab")})`}
              className="text-muted hover:underline"
            >
              {t("common.apiData")}
            </a>
            {(() => {
              const wiki = i18n.resolvedLanguage === "et" ? data.wikipediaUrlEt
                : i18n.resolvedLanguage === "ru" ? data.wikipediaUrlRu
                : data.wikipediaUrlEn;
              if (!wiki) return null;
              return (
                <a
                  href={wiki}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-estonia hover:underline"
                  title={t("profile.wikipediaTooltip", "Wikipedia article about this MP (CC BY-SA)")}
                >
                  {t("profile.wikipedia", "Wikipedia")}
                </a>
              );
            })()}
          </div>
        </div>
      </header>

      <section aria-label={t("a11y.metrics")} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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
        {data.attendanceChecks && (
          <MetricCard
            label={t("profile.metrics.checkPresence")}
            value={pct(data.attendanceChecks.participationRate)}
            hint={t("profile.metrics.checkHint", { present: data.attendanceChecks.attended, total: data.attendanceChecks.totalSittings })}
            sourceUrl={data.attendanceChecks.sourceUrl}
          />
        )}
        <MetricCard
          label={t("profile.metrics.seniority")}
          value={data.parliamentSeniorityDays == null
              ? "—"
              : t("profile.metrics.seniorityYears", { years: Math.floor(data.parliamentSeniorityDays / 365) })}
          hint={t("profile.metrics.seniorityHint")}
          sourceUrl={data.sourceUrl}
        />
      </section>

      {data.election && (
        <section aria-label={t("profile.election.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-4">
            {t("profile.election.title")}
          </h2>
          <div className="flex flex-wrap gap-x-10 gap-y-4">
            <div>
              <div className="font-display font-bold text-[32px] leading-none tracking-[-0.03em] text-ink">
                {data.election.personalVotes.toLocaleString(i18n.resolvedLanguage)}
              </div>
              <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1.5">
                {t("profile.election.personalVotes")}
              </div>
            </div>
            <div>
              <div className="font-display font-bold text-[20px] leading-tight text-ink">
                {t(`profile.election.mandate.${data.election.mandateType}`, data.election.mandateType)}
              </div>
              <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1.5">
                {t("profile.election.mandateType")}
              </div>
            </div>
            {data.election.partyName && (
              <div>
                <div className="text-[15px] text-ink leading-tight">{data.election.partyName}</div>
                <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1.5">
                  {t("profile.election.ranAs")}
                </div>
              </div>
            )}
          </div>
          <a href={data.election.sourceUrl} target="_blank" rel="noreferrer noopener"
             className="inline-block mt-4 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
            {t("profile.election.source")} ↗
          </a>
        </section>
      )}

      {data.activity && (
        <section aria-label={t("profile.activity.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-4">
            {t("profile.activity.title")}
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <ActivityStat value={data.activity.speeches} label={t("profile.activity.speeches")} />
            <ActivityStat value={data.activity.questions} label={t("profile.activity.questions")} />
            <ActivityStat value={data.activity.interpellations} label={t("profile.activity.interpellations")} hint={t("profile.activity.interpellationsHint")} />
            <ActivityStat value={data.activity.writtenQuestions} label={t("profile.activity.writtenQuestions")} />
          </div>
          <a href={data.activity.sourceUrl} target="_blank" rel="noreferrer noopener"
             className="inline-block mt-4 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
            {t("profile.activity.source")} ↗
          </a>
        </section>
      )}

      {(data.education || data.positions) && (
        <section aria-label={t("profile.bio.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
            {t("profile.bio.title")}
          </h2>
          <p className="font-serif italic text-[14px] text-ink-2 mb-4 max-w-[64ch]">
            {t("profile.bio.lede")}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {data.education && (
              <div>
                <h3 className="font-mono text-[11px] uppercase tracking-[0.1em] text-muted mb-2">
                  {t("profile.bio.education")}
                </h3>
                <ul className="flex flex-wrap gap-2">
                  {data.education.split("; ").map((e) => (
                    <li key={e} className="text-[13px] text-ink bg-paper border border-rule rounded-full px-3 py-1">{e}</li>
                  ))}
                </ul>
              </div>
            )}
            {data.positions && (
              <div>
                <h3 className="font-mono text-[11px] uppercase tracking-[0.1em] text-muted mb-2">
                  {t("profile.bio.positions")}
                </h3>
                <ul className="flex flex-wrap gap-2">
                  {data.positions.split("; ").map((p) => (
                    <li key={p} className="text-[13px] text-ink bg-paper border border-rule rounded-full px-3 py-1">{p}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
          <p className="mt-4 font-mono text-[11px] text-muted tracking-[0.04em]">
            {t("profile.bio.source")}
          </p>
        </section>
      )}

      {data.slug && (
        <section aria-label={t("pages.profileEnh.deviationsTitle")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-3">
            {t("pages.profileEnh.deviationsTitle")}
          </h2>
          <DeviationsCalendarSection slug={data.slug} />
        </section>
      )}

      {data.slug && (
        <section aria-label={t("pages.profileEnh.similarPeersTitle")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
            {t("pages.profileEnh.similarPeersTitle")}
          </h2>
          <p className="font-serif italic text-[14px] text-ink-2 mb-4 max-w-[64ch]">
            {t("pages.profileEnh.similarPeersLede")}
          </p>
          <SimilarPeersSection slug={data.slug} />
        </section>
      )}

      {data.groupAlignment && (
        <section aria-label={t("sections.groupAlignment")}>
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
                    const when = d.startedAt ? formatDate(d.startedAt) : "";
                    return (
                      <li key={d.voteEventId} className="p-3 text-sm flex justify-between items-start gap-3">
                        <span className="min-w-0">
                          <Link to={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
                            {d.voteEventDescription ?? t("common.noDescription")}
                          </Link>
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

      {data.externalAffiliations && data.externalAffiliations.length > 0 && (
        <AffiliationTimeline items={data.externalAffiliations} />
      )}

      {data.slug && (
        <section aria-label={t("sections.recentBills")} className="grid grid-cols-1 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] gap-6 items-start">
          <div>
            <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.recentBills")}</h2>
            <div className="border border-rule rounded-[20px] p-5 bg-white">
              <BillsSponsored slug={data.slug} />
            </div>
          </div>
          <div>
            <h2 className="text-lg font-semibold text-ink mb-2">{t("pages.profile.topicRadarHeading")}</h2>
            <div className="border border-rule rounded-[20px] p-5 bg-white">
              <MpTopicRadarSection slug={data.slug} />
            </div>
          </div>
        </section>
      )}

      {data.committees.length > 0 && (
        <section aria-label={t("sections.committees")}>
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.committees")}</h2>
          <ul className="flex flex-wrap gap-2">
            {data.committees.map((c) => (
              <CommitteeChip key={`${c.name}-${c.role}`} c={c} />
            ))}
          </ul>
        </section>
      )}

      {data.slug && (
        <section aria-label={t("sections.recentVotes")}>
          <h2 className="text-lg font-semibold text-ink mb-2">{t("sections.recentVotes")}</h2>
          <VotingHistory slug={data.slug} />
        </section>
      )}

      {data.biographyHtml && (
        <section aria-label={t("sections.biography")}>
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

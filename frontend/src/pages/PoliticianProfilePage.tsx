import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { PressItem, CommitteeMembership, OversightItem } from "../types";
import { fetchProfile } from "../api/politicians";
import { resolveMediaUrl } from "../api/client";
import { fetchPoliticianVotes } from "../api/votes";
import { fetchPoliticianLegislation } from "../api/legislation";
import LoadFailed from "../components/LoadFailed";
import MetricCard from "../components/MetricCard";
import CommitteeChip from "../components/CommitteeChip";
import FactionBadge from "../components/FactionBadge";
import MpTopicRadar from "../components/analytics/MpTopicRadar";
import AffiliationTimeline from "../components/analytics/AffiliationTimeline";
import PartyAffiliationPanel from "../components/PartyAffiliationPanel";
import DeviationsCalendar from "../components/analytics/DeviationsCalendar";
import SimilarPeers from "../components/analytics/SimilarPeers";
import { fetchMpTopicRadar, fetchMpDeviationsTimeline, fetchMpSimilarPeers } from "../api/analytics";
import { formatDate } from "../lib/formatDate";
import { formatPercent } from "../lib/formatNumber";

function pct(v: number | null): string {
  if (v == null) return "—";
  return formatPercent(v);
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

/** Splits a press line "Title // Publication, date, page" into headline + source meta. */
function splitPress(description: string): { headline: string; meta: string | null } {
  const i = description.indexOf("//");
  if (i < 0) return { headline: description.trim(), meta: null };
  const headline = description.slice(0, i).trim();
  const meta = description.slice(i + 2).trim();
  return { headline: headline || description.trim(), meta: meta || null };
}

function PressActivitySection({
  items,
  total,
  officialUrl,
}: {
  items: PressItem[];
  total: number;
  officialUrl: string | null;
}) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const INITIAL = 8;
  const shown = expanded ? items : items.slice(0, INITIAL);
  return (
    <section aria-label={t("profile.press.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
      <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
        {t("profile.press.title")}
      </h2>
      <p className="text-[13px] leading-snug text-ink-2 mb-4">{t("profile.press.lead")}</p>
      <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
        {shown.map((p, i) => {
          const { headline, meta } = splitPress(p.description);
          const when = p.date ? formatDate(p.date) : null;
          return (
            <li key={`${i}-${p.url ?? headline}`} className="p-4">
              {p.url ? (
                <a
                  href={p.url}
                  target="_blank"
                  rel="noreferrer noopener"
                  className="text-ink font-medium text-[15px] leading-snug hover:underline"
                >
                  {headline}
                  <span className="text-blue ml-1 align-baseline">↗</span>
                </a>
              ) : (
                <span className="text-ink font-medium text-[15px] leading-snug">
                  {headline}
                  <span className="ml-2 font-mono text-[9px] tracking-[0.08em] uppercase px-1.5 py-0.5 rounded bg-rule/40 text-muted align-middle">
                    {t("profile.press.printOnly")}
                  </span>
                </span>
              )}
              <div className="text-[12px] leading-snug text-muted mt-1">
                {meta}
                {meta && when ? " · " : ""}
                {when && <span className="font-mono">{when}</span>}
              </div>
            </li>
          );
        })}
      </ul>
      {items.length > INITIAL && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="mt-3 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5"
        >
          {expanded ? t("profile.press.showLess") : t("profile.press.showAll", { count: items.length })}
        </button>
      )}
      {total > items.length && (
        <p className="text-[11px] leading-snug text-muted mt-3">
          {t("profile.press.moreOnOfficial", { total })}{" "}
          {officialUrl && (
            <a href={officialUrl} target="_blank" rel="noreferrer noopener" className="text-blue border-b border-blue">
              riigikogu.ee ↗
            </a>
          )}
        </p>
      )}
      <p className="text-[11px] leading-snug text-muted mt-3 pt-3 border-t border-rule/60">
        {t("profile.press.sourceNote")}
      </p>
    </section>
  );
}

/** Friendship groups, topic support groups and delegations as chip-links to the /groups directory. */
function AffiliationGroups({
  friendship,
  support,
  delegations,
}: {
  friendship: CommitteeMembership[];
  support: CommitteeMembership[];
  delegations: CommitteeMembership[];
}) {
  const { t } = useTranslation();
  const rows: Array<{ key: string; items: CommitteeMembership[] }> = [
    { key: "delegations", items: delegations },
    { key: "friendship", items: friendship },
    { key: "support", items: support },
  ];
  if (rows.every((r) => r.items.length === 0)) return null;
  return (
    <section aria-label={t("profile.affiliations.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
      <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">{t("profile.affiliations.title")}</h2>
      <p className="text-[13px] leading-snug text-ink-2 mb-4 max-w-[72ch]">{t("profile.affiliations.lead")}</p>
      <div className="flex flex-col gap-4">
        {rows.map(({ key, items }) =>
          items.length === 0 ? null : (
            <div key={key}>
              <h3 className="font-mono text-[11px] uppercase tracking-[0.1em] text-muted mb-2">
                {t(`profile.affiliations.${key}`)}
              </h3>
              <ul className="flex flex-wrap gap-2">
                {items.map((g) => (
                  <li key={g.externalId}>
                    <Link
                      to={`/groups/${g.externalId}`}
                      className="inline-block text-[13px] text-ink bg-paper border border-rule rounded-full px-3 py-1 hover:border-blue transition-colors"
                    >
                      {g.name}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )
        )}
      </div>
    </section>
  );
}

function daysBetween(a: string, b: string): number | null {
  const d1 = Date.parse(a);
  const d2 = Date.parse(b);
  if (Number.isNaN(d1) || Number.isNaN(d2)) return null;
  return Math.round((d2 - d1) / 86_400_000);
}

/** Written questions and interpellations the MP put to a minister, with an answer-status signal. */
function OversightSection({ items, total }: { items: OversightItem[]; total: number }) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const INITIAL = 8;
  const shown = expanded ? items : items.slice(0, INITIAL);
  const todayIso = new Date().toISOString().slice(0, 10);
  return (
    <section aria-label={t("profile.oversight.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
      <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">{t("profile.oversight.title")}</h2>
      <p className="text-[13px] leading-snug text-ink-2 mb-4 max-w-[72ch]">{t("profile.oversight.lead")}</p>
      <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
        {shown.map((o, i) => {
          const overdue = !o.answered && !!o.answerDeadline && o.answerDeadline < todayIso;
          const rd = o.answered && o.submittedOn && o.respondedOn ? daysBetween(o.submittedOn, o.respondedOn) : null;
          return (
            <li key={i} className="p-4">
              <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 mb-1">
                <span className="font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded bg-paper border border-rule text-ink-2">
                  {t(`profile.oversight.kind.${o.kind}`, { defaultValue: o.kind })}
                </span>
                {/* Answer status only for written questions: interpellations are answered orally in
                    the sitting, so there is no reliable answer document to derive a status from. */}
                {o.kind === "WRITTEN_QUESTION" && (o.answered ? (
                  <span className="font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded bg-blue/10 text-blue">
                    {rd != null ? t("profile.oversight.answeredIn", { count: rd }) : t("profile.oversight.answered")}
                  </span>
                ) : overdue ? (
                  <span className="font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded bg-hot/10 text-hot-deep">
                    {t("profile.oversight.overdue")}
                  </span>
                ) : (
                  <span className="font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded bg-rule/40 text-muted">
                    {t("profile.oversight.awaiting")}
                  </span>
                ))}
              </div>
              <p className="text-ink font-medium text-[15px] leading-snug">{o.title}</p>
              <div className="text-[12px] leading-snug text-muted mt-1">
                {o.addresseeName && <span>{t("profile.oversight.toMinister", { name: o.addresseeName })}</span>}
                {o.addresseeName && o.submittedOn ? " · " : ""}
                {o.submittedOn && <span className="font-mono">{formatDate(o.submittedOn)}</span>}
              </div>
              <div className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1 font-mono text-[11px] tracking-[0.06em]">
                {o.questionUrl && (
                  <a href={o.questionUrl} target="_blank" rel="noreferrer noopener" className="text-blue border-b border-blue pb-0.5">
                    {t("profile.oversight.readQuestion")} ↗
                  </a>
                )}
                {o.answerUrl && (
                  <a href={o.answerUrl} target="_blank" rel="noreferrer noopener" className="text-blue border-b border-blue pb-0.5">
                    {t("profile.oversight.readAnswer")} ↗
                  </a>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      {items.length > INITIAL && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="mt-3 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5"
        >
          {expanded ? t("profile.oversight.showLess") : t("profile.oversight.showAll", { count: items.length })}
        </button>
      )}
      {total > items.length && (
        <p className="text-[11px] leading-snug text-muted mt-3">
          {t("profile.oversight.moreNote", { total })}
        </p>
      )}
      {items.some((o) => o.kind === "INTERPELLATION") && (
        <p className="text-[11px] leading-snug text-muted mt-3 pt-3 border-t border-rule/60">
          {t("profile.oversight.interpellationNote")}
        </p>
      )}
    </section>
  );
}

function VotingHistory({ slug }: { slug: string }) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["politician-votes", slug],
    queryFn: () => fetchPoliticianVotes(slug, 0, 20),
  });
  if (isLoading) return <p className="text-sm text-slate-500">{t("profile.loadingVotes")}</p>;
  if (error) return <LoadFailed error={error} />;
  if (!data || data.items.length === 0)
    return <p className="text-sm text-slate-500">{t("profile.noVotes")}</p>;
  return (
    <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
      {data.items.map((v) => {
        const when = v.startedAt ? formatDate(v.startedAt) : "";
        return (
          <li key={v.voteEventId} className="flex justify-between items-center p-3 text-sm gap-3">
            <span className="min-w-0">
              {v.billTitle ? (
                <Link to={`/legislation/${encodeURIComponent(v.billId ?? "")}`} className="hover:underline text-ink font-medium">
                  {v.billTitle}
                </Link>
              ) : (
                <Link to={`/votes/${v.voteEventId}`} className="hover:underline text-ink">
                  {v.description ?? t("common.noDescription")}
                </Link>
              )}
              <span className="text-slate-500 block text-xs">
                {v.billTitle && (
                  <Link to={`/votes/${v.voteEventId}`} className="hover:underline">
                    {v.billMark ? `${v.billMark} · ` : ""}{v.description ?? t("common.noDescription")}
                  </Link>
                )}
                {v.billTitle && when ? " · " : ""}{when}
              </span>
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
  if (error) return <LoadFailed error={error} />;
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
  if (error) return <LoadFailed error={error} />;
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
  if (error) return <LoadFailed error={error} />;
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
  if (error) return <LoadFailed error={error} />;
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
  if (error) return <LoadFailed error={error} className="text-red-600" />;
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
            alt={data.fullName}
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
          {/* Shown only for MPs who took their seat mid-term (substitutes / by-election entrants):
              their current mandate starts after the term opened on 2023-04-10. */}
          {data.currentMandateStart && data.currentMandateStart > "2023-04-10" && (
            <div className="text-sm text-slate-600 mt-1">
              {t("profile.currentMandateFrom", { date: formatDate(data.currentMandateStart) })}
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

      {/* Original seat block — how the MP won their Riigikogu seat (unchanged look). */}
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
          {data.election.mandateType === "SUBSTITUTE" && (
            <p className="mt-4 text-[13px] leading-snug text-ink-2 border-l-2 border-rule pl-3">
              {t("profile.election.substituteNote")}
            </p>
          )}
          <a href={data.election.sourceUrl} target="_blank" rel="noreferrer noopener"
             className="inline-block mt-4 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
            {t("profile.election.source")} ↗
          </a>
        </section>
      )}

      {!data.election && data.active && (
        <section aria-label={t("profile.election.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-2">{t("profile.election.title")}</h2>
          <p className="text-[14px] leading-snug text-ink-2">{t("profile.election.substituteUnknown")}</p>
        </section>
      )}

      {/* Added feature — participation in other elections (EP / KOV), complementary to the seat. */}
      {(data.elections ?? []).some((e) => e.electionType !== "RK" && !e.historical) && (
        <section aria-label={t("profile.otherElections.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
            {t("profile.otherElections.title")}
          </h2>
          <p className="text-[13px] leading-snug text-ink-2 mb-4">{t("profile.otherElections.matchNote")}</p>
          <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
            {(data.elections ?? []).filter((e) => e.electionType !== "RK" && !e.historical).map((c) => (
              <li key={c.electionCode} className="p-4 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-baseline gap-x-2.5 gap-y-1">
                    <span className="font-display font-bold text-[16px] tracking-[-0.015em] text-ink">
                      {t(`profile.otherElections.type.${c.electionType}`, { defaultValue: c.electionType })} {c.year}
                    </span>
                    <span className={`font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded ${
                      c.elected ? "bg-blue/10 text-blue" : "bg-rule/40 text-muted"}`}>
                      {t(`profile.otherElections.result.${c.elected ? "elected" : "notElected"}`)}
                    </span>
                  </div>
                  {c.partyName && (
                    <div className="text-[13px] text-ink-2 leading-tight mt-1">
                      {t("profile.otherElections.ranAs")}: {c.partyName}
                    </div>
                  )}
                  <a href={c.sourceUrl} target="_blank" rel="noreferrer noopener"
                     aria-label={`${t("profile.otherElections.source")} (${t("a11y.opensNewTab")})`}
                     className="inline-block mt-1.5 font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
                    {t("profile.otherElections.source")} ↗
                  </a>
                </div>
                <div className="text-right shrink-0">
                  <div className="font-display font-bold text-[24px] leading-none tracking-[-0.03em] text-ink">
                    {c.personalVotes.toLocaleString(i18n.resolvedLanguage)}
                  </div>
                  <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1">
                    {t("profile.otherElections.personalVotes")}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Pre-2023 Riigikogu electoral history — Mölder dataset, attributed, matched by birth date. */}
      {(data.elections ?? []).some((e) => e.historical) && (
        <section aria-label={t("profile.electionHistory.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
            {t("profile.electionHistory.title")}
          </h2>
          <p className="text-[13px] leading-snug text-ink-2 mb-4">{t("profile.electionHistory.lead")}</p>
          <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
            {(data.elections ?? []).filter((e) => e.historical).slice().sort((a, b) => b.year - a.year).map((c) => (
              <li key={c.electionCode} className="p-4 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-baseline gap-x-2.5 gap-y-1">
                    <span className="font-display font-bold text-[16px] tracking-[-0.015em] text-ink">
                      {t("profile.otherElections.type.RK")} {c.year}
                    </span>
                    <span className={`font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded ${
                      c.elected ? "bg-blue/10 text-blue" : "bg-rule/40 text-muted"}`}>
                      {t(`profile.otherElections.result.${c.elected ? "elected" : "notElected"}`)}
                    </span>
                  </div>
                  {c.districtName && (
                    <div className="text-[13px] text-ink-2 leading-tight mt-1">
                      {t("profile.electionHistory.district")}: {c.districtName}
                    </div>
                  )}
                  {c.partyName && (
                    <div className="text-[13px] text-ink-2 leading-tight mt-0.5">
                      {t("profile.otherElections.ranAs")}: {c.partyName}
                    </div>
                  )}
                </div>
                <div className="text-right shrink-0">
                  <div className="font-display font-bold text-[24px] leading-none tracking-[-0.03em] text-ink">
                    {c.personalVotes.toLocaleString(i18n.resolvedLanguage)}
                  </div>
                  <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1">
                    {t("profile.otherElections.personalVotes")}
                  </div>
                </div>
              </li>
            ))}
          </ul>
          <p className="text-[12px] leading-snug text-muted mt-3">
            {t("profile.electionHistory.credit")}{" "}
            <a href="https://www.eestipoliitika.ee" target="_blank" rel="noreferrer noopener"
               className="text-blue border-b border-blue">eestipoliitika.ee</a>
          </p>
          <p className="text-[11px] leading-snug text-muted mt-1.5">{t("profile.electionHistory.grantNote")}</p>
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
          <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 items-baseline">
            <Link to={`/speeches?member=${encodeURIComponent(data.slug ?? "")}&memberName=${encodeURIComponent(data.fullName ?? "")}`}
                  className="font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
              {t("profile.activity.allSpeeches")} →
            </Link>
            <a href={data.activity.sourceUrl} target="_blank" rel="noreferrer noopener"
               className="font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
              {t("profile.activity.source")} ↗
            </a>
          </div>
        </section>
      )}

      {(data.pressActivity ?? []).length > 0 && (
        <PressActivitySection
          items={data.pressActivity}
          total={data.pressTotal ?? data.pressActivity.length}
          officialUrl={data.officialProfileUrl}
        />
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

      {/* Interest declarations live behind e-ID auth in the state register (no open data),
          so this is a pointer, not a mirror — see the body copy for the honest framing. */}
      <section aria-label={t("profile.declaration.title")} className="border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
        <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
          {t("profile.declaration.title")}
        </h2>
        <p className="font-serif italic text-[14px] text-ink-2 mb-3 max-w-[64ch]">
          {t("profile.declaration.body")}
        </p>
        <a href="https://www.emta.ee/eraklient/e-teenused-maksutarkus/registrid-paringud/huvide-deklaratsioon"
           target="_blank" rel="noreferrer noopener"
           className="inline-block font-mono text-[11px] text-blue tracking-[0.06em] border-b border-blue pb-0.5">
          {t("profile.declaration.link")} ↗
        </a>
      </section>

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
              {data.groupAlignment.rate == null ? "—" : formatPercent(data.groupAlignment.rate)}
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
                          {d.billTitle ? (
                            <Link to={`/legislation/${encodeURIComponent(d.billId ?? "")}`} className="hover:underline text-ink font-medium">
                              {d.billTitle}
                            </Link>
                          ) : (
                            <Link to={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
                              {d.voteEventDescription ?? t("common.noDescription")}
                            </Link>
                          )}
                          <span className="text-slate-500 block text-xs">
                            {d.billTitle && (
                              <Link to={`/votes/${d.voteEventId}`} className="hover:underline">
                                {d.billMark ? `${d.billMark} · ` : ""}{d.voteEventDescription ?? t("common.noDescription")}
                              </Link>
                            )}
                            {d.billTitle && when ? " · " : ""}{when}
                          </span>
                        </span>
                        <span className="shrink-0 text-xs text-slate-500 text-right">
                          {t("profile.mpChoice")}: <span className="font-medium text-ink">{d.memberChoice ? t(`choice.${d.memberChoice}`, { defaultValue: d.memberChoice }) : "—"}</span>
                          <br />
                          {t("profile.factionChoice")}: <span className="font-medium text-ink">{d.factionMajorityChoice ? t(`choice.${d.factionMajorityChoice}`, { defaultValue: d.factionMajorityChoice }) : "—"}</span>
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

      <div className="mb-6">
        <PartyAffiliationPanel
          memberships={data.partyMemberships}
          factionName={data.faction?.name ?? null}
          ranForParty={data.election?.partyName ?? null}
        />
      </div>

      {/* Faction (parliamentary group) timeline — shown only when there is a real move between
          factions (e.g. an MP left their party), not for a member who never changed group. */}
      {(data.factionHistory ?? []).length >= 2 && (
        <section aria-label={t("profile.factionHistory.title")} className="mb-6 border border-rule rounded-[22px] p-5 sm:p-6 bg-white">
          <h2 className="font-display font-bold text-[20px] tracking-[-0.02em] mb-1">
            {t("profile.factionHistory.title")}
          </h2>
          <p className="text-[13px] leading-snug text-ink-2 mb-4">{t("profile.factionHistory.lead")}</p>
          <ul className="flex flex-col divide-y divide-rule border border-rule rounded-[16px] overflow-hidden">
            {(data.factionHistory ?? []).slice().reverse().map((f, i) => {
              const current = !f.endDate;
              return (
                <li key={`${f.factionExternalId}-${f.startDate}-${i}`} className="p-4 flex items-center gap-3">
                  <span className={`w-2.5 h-2.5 rounded-full shrink-0 ${current ? "bg-blue" : "bg-rule"}`} aria-hidden />
                  <div className="min-w-0 flex-1">
                    <span className="font-medium text-ink text-[15px] leading-tight">{f.factionName}</span>
                    {current && (
                      <span className="ml-2 font-mono text-[10px] tracking-[0.1em] uppercase px-1.5 py-0.5 rounded bg-blue/10 text-blue align-middle">
                        {t("profile.factionHistory.present")}
                      </span>
                    )}
                  </div>
                  <div className="font-mono text-[11px] text-muted shrink-0 text-right leading-tight">
                    {f.startDate ? formatDate(f.startDate) : "?"} — {f.endDate ? formatDate(f.endDate) : t("profile.factionHistory.present")}
                  </div>
                </li>
              );
            })}
          </ul>
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

      <AffiliationGroups
        friendship={data.friendshipGroups ?? []}
        support={data.supportGroups ?? []}
        delegations={data.delegations ?? []}
      />

      {(data.oversight ?? []).length > 0 && (
        <OversightSection items={data.oversight} total={data.oversightTotal ?? data.oversight.length} />
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

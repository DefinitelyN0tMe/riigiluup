import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchBillSpeeches, fetchLegislationDetail } from "../api/legislation";
import { fetchInitiativesByBill } from "../api/initiatives";
import StageTimeline from "../components/StageTimeline";
import LoadFailed from "../components/LoadFailed";
import TopicChip from "../components/TopicChip";
import { formatDate, formatDateTime } from "../lib/formatDate";
import type { BillSpeech } from "../types";

export default function LegislationDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["legislation", id],
    queryFn: () => fetchLegislationDetail(id!),
    enabled: !!id,
  });
  const initiativesQuery = useQuery({
    queryKey: ["legislation-initiatives", id],
    queryFn: () => fetchInitiativesByBill(id!),
    enabled: !!id,
  });
  const speechesQuery = useQuery({
    queryKey: ["legislation-speeches", id],
    queryFn: () => fetchBillSpeeches(id!),
    enabled: !!id,
  });

  const containerCls = "space-y-6 max-w-[1000px] mx-auto w-full px-5 sm:px-8 md:px-10 py-8 sm:py-12";

  if (isLoading) return <div className={containerCls}><p className="text-slate-500" role="status">{t("common.loading")}</p></div>;
  if (error) return <div className={containerCls}><LoadFailed error={error} className="text-red-600" /></div>;
  if (!data) return <div className={containerCls}><p className="text-slate-500" role="status">{t("common.notFound")}</p></div>;

  const phaseLabel = t(`phase.${data.phase}` as const, { defaultValue: data.phase });
  const initiatedStr = data.initiatedDate
    ? ` · ${t("legislation.initiatedInline", { date: formatDate(data.initiatedDate) })}`
    : "";
  const acceptedStr = data.acceptedDate
    ? ` · ${t("legislation.acceptedInline", { date: formatDate(data.acceptedDate) })}`
    : "";

  return (
    <div className={containerCls}>
      <div>
        <Link to="/legislation" className="text-sm text-estonia hover:underline">{t("legislation.backAll")}</Link>
      </div>

      <header className="space-y-1">
        <h1 className="text-2xl font-semibold text-ink">
          {data.mark != null && <span className="text-slate-400 mr-2">#{data.mark}</span>}
          {data.title}
        </h1>
        <p className="text-sm text-slate-600">
          {data.draftTypeCode ? `${data.draftTypeCode} · ` : ""}
          {phaseLabel}
          {initiatedStr}
          {acceptedStr}
        </p>
        {data.leadingCommitteeName && (
          <p className="text-sm text-slate-500">{t("legislation.leadingCommittee", { name: data.leadingCommitteeName })}</p>
        )}
        <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs">
          <a href={data.riigikoguPageUrl} target="_blank" rel="noopener noreferrer"
             aria-label={`${t("common.riigikoguPage").replace(" ↗", "")} (${t("a11y.opensNewTab")})`}
             className="text-estonia hover:underline">{t("common.riigikoguPage")}</a>
          <a href={data.sourceUrl} target="_blank" rel="noopener noreferrer"
             aria-label={`${t("common.apiData").replace(" ↗", "")} (${t("a11y.opensNewTab")})`}
             className="text-muted hover:underline">{t("common.apiData")}</a>
          {data.rtActId != null && (
            <>
              <a href={`https://www.riigiteataja.ee/akt/${data.rtActId}`} target="_blank" rel="noopener noreferrer"
                 aria-label={`${t("legislation.rtPublication")} (${t("a11y.opensNewTab")})`}
                 className="text-estonia hover:underline">{t("legislation.rtPublication")} ↗</a>
              <a href={`https://www.riigiteataja.ee/akt/${data.rtActId}?leiaKehtiv`} target="_blank" rel="noopener noreferrer"
                 aria-label={`${t("legislation.rtCurrent")} (${t("a11y.opensNewTab")})`}
                 className="text-estonia hover:underline">{t("legislation.rtCurrent")} ↗</a>
            </>
          )}
        </div>
      </header>

      {initiativesQuery.data && initiativesQuery.data.length > 0 && (
        <section aria-label={t("legislation.relatedInitiatives", { count: initiativesQuery.data.length, defaultValue: "Related citizen initiatives" })}>
          <h2 className="text-lg font-semibold text-ink mb-2">
            {t("legislation.relatedInitiatives", { count: initiativesQuery.data.length, defaultValue: "Related citizen initiatives" })}
          </h2>
          <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
            {initiativesQuery.data.map((ini) => (
              <li key={ini.id} className="p-3 text-sm flex items-center justify-between gap-3">
                <Link to={`/initiatives/${ini.id}`} className="min-w-0 truncate hover:underline text-ink">
                  {ini.title ?? `#${ini.externalId}`}
                </Link>
                <span className="shrink-0 text-xs text-slate-500 font-mono">
                  {t("initiatives.signaturesShort", { defaultValue: "{{count}} signatures", count: ini.signatureCount ?? 0 })}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {data.topics.length > 0 && (
        <section aria-label="Topics">
          <div className="flex flex-wrap">
            {data.topics.map((topic) => <TopicChip key={topic.edid} t={topic} />)}
          </div>
        </section>
      )}

      {data.introduction && (
        <section aria-label="Introduction">
          <h2 className="text-lg font-semibold text-ink mb-2">{t("legislation.introduction")}</h2>
          <p className="text-sm text-slate-700 whitespace-pre-line">{data.introduction}</p>
        </section>
      )}

      <section aria-label="Sponsors">
        <h2 className="text-lg font-semibold text-ink mb-2">{t("legislation.sponsors", { count: data.sponsors.length })}</h2>
        <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
          {data.sponsors.map((s, idx) => (
            <li key={s.externalId ?? `${idx}`} className="p-3 text-sm flex justify-between gap-3">
              <span className="min-w-0">
                {s.memberSlug ? (
                  <Link to={`/politicians/${s.memberSlug}`} className="hover:underline text-ink">
                    {s.memberFullName ?? s.displayName}
                  </Link>
                ) : (s.displayName ?? t("legislation.noName"))}
              </span>
              <span className="shrink-0 text-xs text-slate-500">
                {t(`sponsorKind.${s.kind}` as const, { defaultValue: s.kind })}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Timeline">
        <h2 className="text-lg font-semibold text-ink mb-2">{t("legislation.timeline")}</h2>
        <StageTimeline stages={data.stages} />
      </section>

      {data.votes.length > 0 && (
        <section aria-label={t("legislation.votesTitle")}>
          <h2 className="text-lg font-semibold text-ink mb-2">{t("legislation.votesTitle")}</h2>
          <ul className="divide-y divide-slate-200 border border-slate-200 rounded-md">
            {data.votes.map((v) => (
              <li key={v.id}>
                <Link to={`/votes/${v.id}`} className="p-3 flex items-baseline justify-between gap-3 hover:bg-off transition-colors">
                  <span className="min-w-0 text-sm text-ink truncate">{v.description ?? "—"}</span>
                  <span className="shrink-0 flex items-baseline gap-3 font-mono text-[11px] tracking-[0.06em]">
                    <span className="text-muted">{v.startedAt ? formatDate(v.startedAt) : ""}</span>
                    <span><b className="text-blue">{v.resultInFavor}</b> / <b className="text-hot-deep">{v.resultAgainst}</b> / {v.resultAbstained}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {speechesQuery.data && speechesQuery.data.length > 0 && (
        <section aria-label={t("legislation.debatesTitle")}>
          <h2 className="text-lg font-semibold text-ink mb-1">
            {t("legislation.debatesTitle")}
          </h2>
          <p className="text-xs text-slate-500 mb-3">{t("legislation.debatesNote")}</p>
          <div className="space-y-5">
            {groupByAgenda(speechesQuery.data).map((group) => (
              <div key={group.title} className="border border-slate-200 rounded-md overflow-hidden">
                <div className="bg-off px-3 py-2 font-mono text-[10px] tracking-[0.1em] uppercase text-muted">
                  {group.title}
                </div>
                <ul className="divide-y divide-slate-200">
                  {group.speeches.map((s) => (
                    <li key={s.id} className="p-3">
                      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1 mb-1">
                        {s.memberSlug ? (
                          <Link to={`/politicians/${encodeURIComponent(s.memberSlug)}`}
                                className="text-sm font-semibold text-blue hover:underline">
                            {s.memberName ?? s.speaker}
                          </Link>
                        ) : (
                          <span className="text-sm font-semibold text-ink">{s.speaker}</span>
                        )}
                        <span className="font-mono text-[11px] text-muted tracking-[0.06em]">
                          {formatDateTime(s.spokenAt)}
                        </span>
                      </div>
                      <p className="text-sm text-slate-700 line-clamp-3">{s.excerpt}</p>
                      <a href={s.sourceUrl} target="_blank" rel="noopener noreferrer"
                         aria-label={`${t("speeches.openStenogram")} (${t("a11y.opensNewTab")})`}
                         className="inline-block mt-1.5 font-mono text-[11px] text-estonia hover:underline">
                        {t("speeches.openStenogram")} ↗
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

/** Group a bill's speeches by agenda-item title (i.e. by reading), preserving first-seen order. */
function groupByAgenda(speeches: BillSpeech[]): { title: string; speeches: BillSpeech[] }[] {
  const groups: { title: string; speeches: BillSpeech[] }[] = [];
  const byTitle = new Map<string, BillSpeech[]>();
  for (const s of speeches) {
    const title = s.agendaItemTitle ?? "";
    let bucket = byTitle.get(title);
    if (!bucket) {
      bucket = [];
      byTitle.set(title, bucket);
      groups.push({ title, speeches: bucket });
    }
    bucket.push(s);
  }
  return groups;
}

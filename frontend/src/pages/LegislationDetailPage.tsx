import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchLegislationDetail } from "../api/legislation";
import { fetchInitiativesByBill } from "../api/initiatives";
import StageTimeline from "../components/StageTimeline";
import TopicChip from "../components/TopicChip";
import { formatDate } from "../lib/formatDate";

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

  if (isLoading) return <p className="text-slate-500" role="status">{t("common.loading")}</p>;
  if (error) return <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500" role="status">{t("common.notFound")}</p>;

  const phaseLabel = t(`phase.${data.phase}` as const, { defaultValue: data.phase });
  const initiatedStr = data.initiatedDate
    ? ` · ${t("legislation.initiatedInline", { date: formatDate(data.initiatedDate) })}`
    : "";
  const acceptedStr = data.acceptedDate
    ? ` · ${t("legislation.acceptedInline", { date: formatDate(data.acceptedDate) })}`
    : "";

  return (
    <div className="space-y-6">
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
        <section aria-label={t("legislation.startedAsInitiative", { defaultValue: "Started as a citizen initiative" })}>
          <h2 className="text-lg font-semibold text-ink mb-2">
            {t("legislation.startedAsInitiative", { defaultValue: "Started as a citizen initiative" })}
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
    </div>
  );
}

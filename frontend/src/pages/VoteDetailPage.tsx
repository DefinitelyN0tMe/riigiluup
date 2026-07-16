import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchVoteDetail } from "../api/votes";
import VoteResultBar from "../components/VoteResultBar";
import VoteDefectorsPanel from "../components/analytics/VoteDefectorsPanel";
import { formatDateTime } from "../lib/formatDate";

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
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ["vote", id],
    queryFn: () => fetchVoteDetail(id!),
    enabled: !!id,
  });

  if (isLoading) return <p className="text-slate-500" role="status">{t("common.loading")}</p>;
  if (error) return <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>;
  if (!data) return <p className="text-slate-500" role="status">{t("common.notFound")}</p>;

  const when = formatDateTime(data.startedAt);
  const typeLabel = t(`voteType.${data.type}` as const, { defaultValue: data.type });

  const total = data.resultInFavor + data.resultAgainst + data.resultAbstained + data.resultPresent + data.resultAbsent;
  const quorum = 51;
  const margin = data.resultInFavor - data.resultAgainst;
  const marginPct = total ? (margin / total) * 100 : 0;
  const tight = Math.abs(margin) < 10 && data.resultInFavor > 0 && data.resultAgainst > 0;

  return (
    <div className="space-y-6 max-w-[1200px] mx-auto w-full px-5 sm:px-8 md:px-10 py-8 sm:py-12">
      <div>
        <Link to="/votes" className="text-sm text-blue hover:underline font-mono tracking-[0.06em]">{t("votes.backAll")}</Link>
      </div>

      <header className="space-y-1">
        <h1 className="text-2xl font-semibold text-ink">{data.description ?? t("common.noDescription")}</h1>
        <p className="text-sm text-slate-600">
          {t("votes.meta", { type: typeLabel, number: data.votingNumber ?? "—", when })}
        </p>
        {data.sittingTitle && (
          <p className="text-sm text-slate-500">{t("votes.sitting", { title: data.sittingTitle })}</p>
        )}
        <a
          href={data.sourceUrl} target="_blank" rel="noopener noreferrer"
          aria-label={`${t("common.riigikoguSource").replace(" ↗", "")} (${t("a11y.opensNewTab")})`}
          className="text-xs text-estonia hover:underline inline-block mt-1"
        >{t("common.riigikoguSource")}</a>
        {data.linkedBill && (
          <div className="mt-2 text-sm">
            {t("votes.bill")} <Link to={`/legislation/${data.linkedBill.id}`} className="text-estonia hover:underline">
              {data.linkedBill.mark != null && <span className="text-slate-400 mr-1">#{data.linkedBill.mark}</span>}
              {data.linkedBill.title}
            </Link>
          </div>
        )}
      </header>

      <section aria-label={t("a11y.result")} className="grid grid-cols-1 md:grid-cols-[2fr_1fr] gap-6 items-start">
        <div>
          <VoteResultBar
            inFavor={data.resultInFavor}
            against={data.resultAgainst}
            abstained={data.resultAbstained}
            didNotVote={data.resultPresent}
            absent={data.resultAbsent}
          />
        </div>
        {data.type === "OPEN" && total > 0 && (
          <div className={`p-5 rounded-[20px] border ${tight ? "border-hot bg-hot/5" : "border-rule bg-white"}`}>
            <div className="font-mono text-[10px] tracking-[0.18em] uppercase text-muted flex items-center gap-2">
              {tight && <span className="w-1.5 h-1.5 rounded-full bg-hot animate-pulse-dot" />}
              {t("pages.voteDetail.margin")}
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className={`font-display font-bold text-[42px] leading-none tracking-[-0.03em] ${margin >= 0 ? "text-blue" : "text-hot"}`}>
                {margin >= 0 ? "+" : ""}{margin}
              </span>
              <span className="font-serif italic text-[16px] text-muted">
                {t("pages.voteDetail.voteWord")} ({marginPct >= 0 ? "+" : ""}{marginPct.toFixed(1)}%)
              </span>
            </div>
            <div className="mt-3 relative h-2 rounded-full bg-off overflow-hidden">
              <div className="absolute inset-y-0 left-0 bg-blue" style={{ width: `${(data.resultInFavor / total) * 100}%` }} />
              <div className="absolute inset-y-0 right-0 bg-hot" style={{ width: `${(data.resultAgainst / total) * 100}%` }} />
              <div className="absolute inset-y-[-4px] w-[2px] bg-ink" style={{ left: `${(quorum / total) * 100}%` }} title={t("pages.voteDetail.quorum")} />
            </div>
            <div className="font-mono text-[10px] tracking-[0.06em] uppercase text-muted mt-2 flex justify-between">
              <span>{t("pages.voteDetail.forShort")} <b className="text-blue">{data.resultInFavor}</b></span>
              <span>{t("pages.voteDetail.quorum")}</span>
              <span>{t("pages.voteDetail.againstShort")} <b className="text-hot">{data.resultAgainst}</b></span>
            </div>
            {tight && (
              <div className="mt-3 font-serif italic text-[13px] text-hot leading-snug">
                {t("pages.voteDetail.tightNote")}
              </div>
            )}
          </div>
        )}
      </section>

      <VoteDefectorsPanel v={data} />

      <section aria-label={t("votes.breakdown")}>
        <h2 className="text-lg font-semibold text-ink mb-2">{t("votes.breakdown")}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {data.factionBreakdowns.map((f) => (
            <div key={f.factionExternalId ?? f.factionName}
                 className="border border-slate-200 rounded-lg p-3">
              <div className="flex justify-between items-baseline mb-2">
                <span className="font-medium truncate">{f.factionName}</span>
                <span className="text-xs text-slate-500 shrink-0">{t("votes.membersShort", { count: f.total })}</span>
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

      <section aria-label={t("a11y.individualVotes")}>
        <h2 className="text-lg font-semibold text-ink mb-2">
          {t("votes.individual", { count: data.individualVotes.length })}
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
                <span className="text-slate-500"> — {iv.factionName ?? t("common.unaffiliated")}</span>
              </span>
              <span className={`shrink-0 font-medium ${CHOICE_CLASS[iv.choice] ?? ""}`}>
                {t(`choice.${iv.choice}` as const, { defaultValue: iv.choice })}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

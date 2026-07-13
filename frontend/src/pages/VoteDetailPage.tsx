import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchVoteDetail } from "../api/votes";
import VoteResultBar from "../components/VoteResultBar";

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

  const when = data.startedAt ? new Date(data.startedAt).toLocaleString() : "—";
  const typeLabel = t(`voteType.${data.type}` as const, { defaultValue: data.type });

  return (
    <div className="space-y-6">
      <div>
        <Link to="/votes" className="text-sm text-estonia hover:underline">{t("votes.backAll")}</Link>
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
          aria-label="Riigikogu source (opens in new tab)"
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

      <section aria-label="Individual votes">
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

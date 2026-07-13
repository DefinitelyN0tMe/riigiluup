import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { VoteListItem } from "../types";
import VoteResultBar from "./VoteResultBar";

const SHORT_TYPE_KEY: Record<VoteListItem["type"], string> = {
  OPEN: "voteType.OPEN",
  ATTENDANCE_CHECK: "voteType.ATTENDANCE_SHORT",
  SECRET: "voteType.SECRET",
  OTHER: "voteType.OTHER",
};

export default function VoteRow({ v }: { v: VoteListItem }) {
  const { t } = useTranslation();
  const when = v.startedAt ? new Date(v.startedAt).toLocaleString() : "";
  return (
    <Link
      to={`/votes/${encodeURIComponent(v.id)}`}
      className="block border border-slate-200 rounded-lg p-4 hover:shadow-sm hover:border-estonia transition"
    >
      <div className="flex justify-between items-start gap-3 mb-2">
        <div className="min-w-0">
          <h2 className="text-base font-semibold text-ink truncate">{v.description ?? t("common.noDescription")}</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {t(SHORT_TYPE_KEY[v.type], { defaultValue: v.type })} · {when}
          </p>
        </div>
        <span className="text-xs text-slate-500 shrink-0">#{v.votingNumber ?? "—"}</span>
      </div>
      <VoteResultBar
        inFavor={v.resultInFavor}
        against={v.resultAgainst}
        abstained={v.resultAbstained}
        didNotVote={v.resultPresent}
        absent={v.resultAbsent}
      />
    </Link>
  );
}

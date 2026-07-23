import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { VoteListItem } from "../types";
import VoteResultBar from "./VoteResultBar";
import { formatDateTime } from "../lib/formatDate";

const SHORT_TYPE_KEY: Record<VoteListItem["type"], string> = {
  OPEN: "voteType.OPEN",
  ATTENDANCE_CHECK: "voteType.ATTENDANCE_SHORT",
  SECRET: "voteType.SECRET",
  OTHER: "voteType.OTHER",
};

export default function VoteRow({ v }: { v: VoteListItem }) {
  const { t } = useTranslation();
  const when = v.startedAt
    ? formatDateTime(v.startedAt, { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" })
    : "";
  return (
    <Link
      to={`/votes/${encodeURIComponent(v.id)}`}
      className="block bg-white border border-rule rounded-[20px] p-4 sm:p-5 hover:-translate-y-0.5 hover:border-blue transition-all"
    >
      <div className="flex justify-between items-start gap-3 mb-3">
        <div className="min-w-0">
          <h2 className="font-display font-bold text-[16px] sm:text-[18px] leading-[1.2] tracking-[-0.02em] line-clamp-2">
            {v.billTitle ?? v.description ?? t("common.noDescription")}
          </h2>
          <p className="font-mono text-[11px] text-muted mt-1 tracking-[0.04em]">
            {v.billTitle && v.description ? `${v.description} · ` : ""}
            {t(SHORT_TYPE_KEY[v.type], { defaultValue: v.type })} · {when}
          </p>
        </div>
        <span className="font-mono text-[11px] font-bold text-blue shrink-0">#{v.votingNumber ?? "—"}</span>
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

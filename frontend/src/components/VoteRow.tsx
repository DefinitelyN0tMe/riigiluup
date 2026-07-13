import { Link } from "react-router-dom";
import type { VoteListItem } from "../types";
import VoteResultBar from "./VoteResultBar";

const TYPE_LABEL: Record<VoteListItem["type"], string> = {
  OPEN: "Roll-call",
  ATTENDANCE_CHECK: "Attendance",
  SECRET: "Secret",
  OTHER: "Other",
};

export default function VoteRow({ v }: { v: VoteListItem }) {
  const when = v.startedAt ? new Date(v.startedAt).toLocaleString() : "";
  return (
    <Link
      to={`/votes/${encodeURIComponent(v.id)}`}
      className="block border border-slate-200 rounded-lg p-4 hover:shadow-sm hover:border-estonia transition"
    >
      <div className="flex justify-between items-start gap-3 mb-2">
        <div className="min-w-0">
          <h3 className="font-semibold text-ink truncate">{v.description ?? "(no description)"}</h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {TYPE_LABEL[v.type]} · {when}
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

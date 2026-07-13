import { Link } from "react-router-dom";
import type { ComparisonDisagreement } from "../types";

const CHOICE_LABEL: Record<string, string> = {
  FOR: "For",
  AGAINST: "Against",
  ABSTAINED: "Abstained",
  DID_NOT_VOTE: "Did not vote",
  ABSENT: "Absent",
  PRESENT: "Present",
  UNKNOWN: "Unknown",
};

const CHOICE_CLASS: Record<string, string> = {
  FOR: "text-estonia",
  AGAINST: "text-orange-600",
  ABSTAINED: "text-slate-600",
  DID_NOT_VOTE: "text-slate-500",
  ABSENT: "text-slate-400",
  PRESENT: "text-estonia",
  UNKNOWN: "text-slate-400",
};

export default function DisagreementRow({ d }: { d: ComparisonDisagreement }) {
  const when = d.startedAt ? new Date(d.startedAt).toLocaleDateString() : "";
  return (
    <li className="p-3 text-sm">
      <div className="flex justify-between items-start gap-3">
        <span className="min-w-0">
          <Link to={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
            {d.voteEventDescription ?? "(no description)"}
          </Link>
          <span className="text-slate-500 block text-xs">{when}</span>
        </span>
        <span className="shrink-0 text-xs flex flex-col items-end gap-1">
          <span className={CHOICE_CLASS[d.leftChoice] ?? ""}>Left: {CHOICE_LABEL[d.leftChoice] ?? d.leftChoice}</span>
          <span className={CHOICE_CLASS[d.rightChoice] ?? ""}>Right: {CHOICE_LABEL[d.rightChoice] ?? d.rightChoice}</span>
        </span>
      </div>
    </li>
  );
}

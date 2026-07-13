import { Link } from "react-router-dom";
import type { LegislationListItem } from "../types";

const PHASE_LABEL: Record<LegislationListItem["phase"], string> = {
  SUBMITTED: "Submitted",
  IN_COMMITTEE: "In committee",
  IN_READINGS: "In readings",
  ADOPTED: "Adopted",
  REJECTED: "Rejected",
  WITHDRAWN: "Withdrawn",
  OTHER: "Other",
};

const PHASE_CLASS: Record<LegislationListItem["phase"], string> = {
  SUBMITTED: "bg-slate-100 text-slate-700",
  IN_COMMITTEE: "bg-amber-100 text-amber-800",
  IN_READINGS: "bg-blue-100 text-blue-800",
  ADOPTED: "bg-emerald-100 text-emerald-800",
  REJECTED: "bg-red-100 text-red-800",
  WITHDRAWN: "bg-slate-100 text-slate-500",
  OTHER: "bg-slate-100 text-slate-500",
};

export default function LegislationRow({ i }: { i: LegislationListItem }) {
  const initiated = i.initiatedDate ? new Date(i.initiatedDate).toLocaleDateString() : "—";
  return (
    <Link
      to={`/legislation/${encodeURIComponent(i.id)}`}
      className="block border border-slate-200 rounded-lg p-4 hover:shadow-sm hover:border-estonia transition"
    >
      <div className="flex justify-between items-start gap-3">
        <div className="min-w-0">
          <h3 className="font-semibold text-ink truncate">
            {i.mark != null && <span className="text-slate-400 mr-2">#{i.mark}</span>}
            {i.title}
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {i.draftTypeCode ? `${i.draftTypeCode} · ` : ""}
            Initiated {initiated}
            {i.leadingCommitteeName ? ` · ${i.leadingCommitteeName}` : ""}
          </p>
        </div>
        <span className={`shrink-0 px-2 py-0.5 rounded text-xs ${PHASE_CLASS[i.phase]}`}>
          {PHASE_LABEL[i.phase]}
        </span>
      </div>
    </Link>
  );
}

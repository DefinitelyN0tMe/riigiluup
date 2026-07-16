import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ComparisonDisagreement } from "../types";
import { formatDate } from "../lib/formatDate";

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
  const { t } = useTranslation();
  const when = d.startedAt ? formatDate(d.startedAt) : "";
  const leftLabel = t(`choice.${d.leftChoice}` as const, { defaultValue: d.leftChoice });
  const rightLabel = t(`choice.${d.rightChoice}` as const, { defaultValue: d.rightChoice });
  return (
    <li className="p-3 text-sm">
      <div className="flex justify-between items-start gap-3">
        <span className="min-w-0">
          <Link to={`/votes/${d.voteEventId}`} className="hover:underline text-ink">
            {d.voteEventDescription ?? t("common.noDescription")}
          </Link>
          <span className="text-slate-500 block text-xs">{when}</span>
        </span>
        <span className="shrink-0 text-xs flex flex-col items-end gap-1">
          <span className={CHOICE_CLASS[d.leftChoice] ?? ""}>{t("compare.left")}: {leftLabel}</span>
          <span className={CHOICE_CLASS[d.rightChoice] ?? ""}>{t("compare.right")}: {rightLabel}</span>
        </span>
      </div>
    </li>
  );
}

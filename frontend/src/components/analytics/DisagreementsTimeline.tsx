import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ComparisonDisagreement } from "../../types";

/**
 * Editorial-style vertical timeline of disagreements between two MPs.
 * Big serif italic dates on the left, vote title + per-MP choice pills on the right.
 */
export default function DisagreementsTimeline({
  items, leftName, rightName,
}: {
  items: ComparisonDisagreement[];
  leftName: string;
  rightName: string;
}) {
  const { t, i18n } = useTranslation();
  if (items.length === 0) {
    return (
      <p className="font-serif italic text-[16px] text-ink-2">
        {t("compare.noDisagreements")}
      </p>
    );
  }
  return (
    <ol className="flex flex-col gap-6 sm:gap-8 border-l-2 border-blue pl-6 sm:pl-8">
      {items.map((d) => {
        const dt = d.startedAt ? new Date(d.startedAt) : null;
        const day = dt ? dt.toLocaleDateString(i18n.resolvedLanguage, { day: "2-digit" }) : "—";
        const month = dt ? dt.toLocaleDateString(i18n.resolvedLanguage, { month: "long" }) : "";
        const year = dt ? dt.getFullYear() : "";
        return (
          <li key={d.voteEventId} className="relative">
            <span aria-hidden className="absolute -left-[35px] sm:-left-[39px] top-1.5 w-4 h-4 rounded-full bg-white border-[3px] border-blue" />
            <div className="grid grid-cols-1 sm:grid-cols-[140px_1fr] gap-3 sm:gap-6 items-start">
              <div className="sm:text-right">
                <div className="font-serif italic font-light text-[42px] sm:text-[52px] leading-none tracking-[-0.03em] text-blue">
                  {day}
                </div>
                <div className="font-mono text-[10px] tracking-[0.18em] uppercase text-muted mt-1">
                  {month} {year}
                </div>
              </div>
              <div>
                <Link to={`/votes/${encodeURIComponent(d.voteEventId)}`}
                      className="font-display font-bold text-[18px] sm:text-[20px] tracking-[-0.02em] leading-snug hover:text-blue transition-colors">
                  {d.voteEventDescription ?? t("common.noDescription")}
                </Link>
                {d.voteType && (
                  <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1">
                    {t(`voteType.${d.voteType}` as const, { defaultValue: d.voteType })}
                  </div>
                )}
                <div className="mt-3 flex flex-wrap gap-2 font-mono text-[11px]">
                  <ChoicePill label={leftName} choice={d.leftChoice} side="left" />
                  <ChoicePill label={rightName} choice={d.rightChoice} side="right" />
                </div>
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

function ChoicePill({ label, choice, side }: { label: string; choice: string; side: "left" | "right" }) {
  const { t } = useTranslation();
  const c = choice as "FOR" | "AGAINST" | "ABSTAINED" | "DID_NOT_VOTE" | "ABSENT" | "PRESENT" | "UNKNOWN";
  const color =
    c === "FOR" ? "bg-blue text-white" :
    c === "AGAINST" ? "bg-hot text-white" :
    c === "ABSTAINED" ? "bg-ink text-white" :
    "bg-off text-ink border border-rule";
  return (
    <span className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 ${color}`}>
      <span className="font-serif italic text-[10px] opacity-70">{side === "left" ? "L" : "R"}</span>
      <span className="font-medium">{label}</span>
      <span className="font-bold tracking-[0.06em] uppercase text-[10px]">
        {t(`choice.${c}` as const, { defaultValue: c })}
      </span>
    </span>
  );
}

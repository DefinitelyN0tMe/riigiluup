import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchComparison } from "../api/comparisons";
import { resolveMediaUrl } from "../api/client";
import AgreementBar from "./AgreementBar";
import DisagreementsTimeline from "./analytics/DisagreementsTimeline";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

/**
 * Full comparison view — reused by both the standalone /compare route
 * and inline on /politicians when the user picks 2 MPs from the grid.
 * Both slugs required; parent gates rendering behind `if (left && right)`.
 */
export default function ComparisonPanel({
  leftSlug,
  rightSlug,
}: {
  leftSlug: string;
  rightSlug: string;
}) {
  const { t } = useTranslation();
  const { data, isLoading, error } = useQuery({
    queryKey: ["comparison", leftSlug, rightSlug],
    queryFn: () => fetchComparison(leftSlug, rightSlug),
  });

  if (isLoading) return <p className="text-slate-500" role="status">{t("compare.computing")}</p>;
  if (error) return <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <section aria-label="Header" className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {[data.left, data.right].map((side, idx) => (
          <div key={idx} className="border border-slate-200 rounded-lg p-4 flex items-start gap-3 bg-white">
            {resolveMediaUrl(side.photoUrl) ? (
              <img src={resolveMediaUrl(side.photoUrl)} alt=""
                   className="w-16 h-16 rounded-full object-cover bg-slate-100" />
            ) : (
              <div className="w-16 h-16 rounded-full bg-slate-100" />
            )}
            <div className="min-w-0">
              <h3 className="text-lg font-semibold text-ink truncate">{side.fullName}</h3>
              <p className="text-sm text-slate-600 truncate">
                {side.factionName ?? t("common.unaffiliated")}
                {side.partyShortName && <span className="text-slate-500"> — {side.partyShortName}</span>}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                {t("compare.groupAlignmentLine")} <span className="font-medium text-ink">{pct(side.groupAlignmentRate)}</span>
                <span className="text-slate-500"> {t("compare.groupAlignmentCounts", { matches: side.groupAlignmentMatches, eligible: side.groupAlignmentEligible })}</span>
              </p>
            </div>
          </div>
        ))}
      </section>

      <section aria-label="Agreement">
        <h3 className="text-lg font-semibold text-ink mb-2">{t("compare.voteAgreement")}</h3>
        <p className="text-3xl font-semibold text-ink mb-2">{pct(data.agreement.agreementRate)}</p>
        <AgreementBar
          same={data.agreement.sameCount}
          diff={data.agreement.diffCount}
          oneNotParticipating={data.agreement.oneNotParticipatingCount}
        />
        <p className="text-xs text-slate-500 mt-2">{data.agreement.methodologyNote}</p>
      </section>

      <section aria-label="Recent disagreements">
        <h3 className="font-display font-bold text-[22px] tracking-[-0.02em] mb-1">
          {t("compare.recentDisagreements", { count: data.recentDisagreements.length })}
        </h3>
        <p className="font-serif italic text-[15px] text-ink-2 mb-6">
          {t("compare.disagreementsLede")}
        </p>
        <DisagreementsTimeline
          items={data.recentDisagreements}
          leftName={data.left.fullName}
          rightName={data.right.fullName}
        />
      </section>
    </div>
  );
}

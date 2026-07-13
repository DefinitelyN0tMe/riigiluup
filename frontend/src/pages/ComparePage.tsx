import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchComparison } from "../api/comparisons";
import { resolveMediaUrl } from "../api/client";
import MpPicker from "../components/MpPicker";
import AgreementBar from "../components/AgreementBar";
import DisagreementRow from "../components/DisagreementRow";

function pct(v: number | null): string {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

export default function ComparePage() {
  const { t } = useTranslation();
  const [leftSlug, setLeftSlug] = useState<string | null>(null);
  const [rightSlug, setRightSlug] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["comparison", leftSlug, rightSlug],
    queryFn: () => fetchComparison(leftSlug!, rightSlug!),
    enabled: !!(leftSlug && rightSlug),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-ink">{t("compare.title")}</h1>

      <div className="flex flex-wrap gap-4">
        <MpPicker label={t("compare.leftMp")} value={leftSlug} onChange={setLeftSlug} />
        <MpPicker label={t("compare.rightMp")} value={rightSlug} onChange={setRightSlug} />
      </div>

      {!leftSlug || !rightSlug ? (
        <p className="text-sm text-slate-500">{t("compare.prompt")}</p>
      ) : isLoading ? (
        <p className="text-slate-500" role="status">{t("compare.computing")}</p>
      ) : error ? (
        <p className="text-red-600" role="alert">{t("common.failedToLoad")} {(error as Error).message}</p>
      ) : data ? (
        <>
          <section aria-label="Header" className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[data.left, data.right].map((side, idx) => (
              <div key={idx} className="border border-slate-200 rounded-lg p-4 flex items-start gap-3">
                {resolveMediaUrl(side.photoUrl) ? (
                  <img src={resolveMediaUrl(side.photoUrl)} alt=""
                       className="w-16 h-16 rounded-full object-cover bg-slate-100" />
                ) : (
                  <div className="w-16 h-16 rounded-full bg-slate-100" />
                )}
                <div className="min-w-0">
                  <h2 className="text-lg font-semibold text-ink truncate">{side.fullName}</h2>
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
            <h2 className="text-lg font-semibold text-ink mb-2">{t("compare.voteAgreement")}</h2>
            <p className="text-3xl font-semibold text-ink mb-2">{pct(data.agreement.agreementRate)}</p>
            <AgreementBar
              same={data.agreement.sameCount}
              diff={data.agreement.diffCount}
              oneNotParticipating={data.agreement.oneNotParticipatingCount}
            />
            <p className="text-xs text-slate-500 mt-2">{data.agreement.methodologyNote}</p>
          </section>

          <section aria-label="Recent disagreements">
            <h2 className="text-lg font-semibold text-ink mb-2">
              {t("compare.recentDisagreements", { count: data.recentDisagreements.length })}
            </h2>
            {data.recentDisagreements.length === 0 ? (
              <p className="text-sm text-slate-500">{t("compare.noDisagreements")}</p>
            ) : (
              <ul className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
                {data.recentDisagreements.map((d) => (
                  <DisagreementRow key={d.voteEventId} d={d} />
                ))}
              </ul>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}

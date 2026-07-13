import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
  const [leftSlug, setLeftSlug] = useState<string | null>(null);
  const [rightSlug, setRightSlug] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["comparison", leftSlug, rightSlug],
    queryFn: () => fetchComparison(leftSlug!, rightSlug!),
    enabled: !!(leftSlug && rightSlug),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-ink">Compare two MPs</h1>

      <div className="flex flex-wrap gap-4">
        <MpPicker label="Left MP" value={leftSlug} onChange={setLeftSlug} />
        <MpPicker label="Right MP" value={rightSlug} onChange={setRightSlug} />
      </div>

      {!leftSlug || !rightSlug ? (
        <p className="text-sm text-slate-500">Pick two MPs to see how they vote together.</p>
      ) : isLoading ? (
        <p className="text-slate-500">Computing…</p>
      ) : error ? (
        <p className="text-red-600">Failed to load. {(error as Error).message}</p>
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
                    {side.factionName ?? "Unaffiliated"}
                    {side.partyShortName && <span className="text-slate-500"> — {side.partyShortName}</span>}
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    Group alignment: <span className="font-medium text-ink">{pct(side.groupAlignmentRate)}</span>
                    <span className="text-slate-500"> ({side.groupAlignmentMatches}/{side.groupAlignmentEligible})</span>
                  </p>
                </div>
              </div>
            ))}
          </section>

          <section aria-label="Agreement">
            <h2 className="text-lg font-semibold text-ink mb-2">Vote agreement</h2>
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
              Recent disagreements ({data.recentDisagreements.length})
            </h2>
            {data.recentDisagreements.length === 0 ? (
              <p className="text-sm text-slate-500">No comparable disagreements in the current window.</p>
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

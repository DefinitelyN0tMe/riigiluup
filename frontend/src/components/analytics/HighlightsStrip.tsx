import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { HighlightsBundle } from "../../api/analytics";

export default function HighlightsStrip({ data }: { data: HighlightsBundle }) {
  const { t } = useTranslation();
  return (
    <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)] gap-4 sm:gap-5">
      <div className="bg-white border border-rule rounded-[22px] p-6 sm:p-7">
        <div className="font-mono text-[10px] tracking-[0.2em] uppercase text-blue font-bold">
          {t("viz.highlights.streaks")}
        </div>
        <div className="font-display font-bold h-display-md mt-3">{data.streaks.length}</div>
        <div className="font-serif italic text-muted text-sm">
          {t("viz.highlights.streaksBody")}
        </div>
        {data.streaks.length > 0 && (
          <ul className="mt-4 flex flex-wrap gap-1.5 text-[11px]">
            {data.streaks.slice(0, 12).map((s) => (
              <li key={s.memberSlug}>
                <Link to={`/politicians/${encodeURIComponent(s.memberSlug)}`}
                  className="bg-off border border-rule rounded-full px-2.5 py-1 font-medium hover:border-blue text-ink">
                  {s.memberName}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="bg-white border border-rule rounded-[22px] p-6 sm:p-7">
        <div className="font-mono text-[10px] tracking-[0.2em] uppercase text-blue font-bold">
          {t("viz.highlights.tightest")}
        </div>
        <div className="font-serif italic text-muted text-sm mt-2">
          {t("viz.highlights.tightestBody")}
        </div>
        <ul className="mt-4 flex flex-col divide-y divide-rule">
          {data.tightVotes.map((v) => {
            const total = v.forCount + v.againstCount;
            const forPct = total ? (v.forCount / total) * 100 : 50;
            return (
              <li key={v.voteId}>
                <Link to={`/votes/${encodeURIComponent(v.voteId)}`} className="block py-2.5 hover:bg-off px-2 -mx-2 rounded">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[11px] font-bold text-hot-deep">±{v.margin}</span>
                    <span className="font-display text-sm font-semibold tracking-[-0.015em] truncate flex-1">
                      {v.description ?? "—"}
                    </span>
                  </div>
                  <div className="mt-1.5 h-1.5 rounded-full bg-off overflow-hidden flex">
                    <div className="bg-blue" style={{ width: `${forPct}%` }} />
                    <div className="bg-hot" style={{ width: `${100 - forPct}%` }} />
                  </div>
                  <div className="font-mono text-[10px] text-muted mt-1 flex justify-between">
                    <span><b className="text-ink font-bold">{v.forCount}</b> {t("viz.highlights.for")}</span>
                    <span><b className="text-ink font-bold">{v.againstCount}</b> {t("viz.highlights.against")}</span>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

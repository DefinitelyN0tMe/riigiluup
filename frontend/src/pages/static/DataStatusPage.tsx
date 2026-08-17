import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../../api/politicians";
import { formatDateTime } from "../../lib/formatDate";
import LoadFailed from "../../components/LoadFailed";

export default function DataStatusPage() {
  const { t } = useTranslation();
  const { data, isPending, isError, error } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });

  return (
    <div className="max-w-[1200px] mx-auto w-full px-5 sm:px-8 md:px-10 py-8 sm:py-12">
      <div className="font-mono text-[11px] tracking-[0.2em] uppercase text-blue font-bold flex items-center gap-2.5 mb-4">
        <span className="bg-blue text-white px-2 py-0.5 rounded font-bold tracking-[0.14em]">i.</span>
        {t("dataStatus.kicker")}
      </div>
      <h1 className="font-display font-bold h-display-lg mb-5">
        {t("dataStatus.title")}
      </h1>

      {/* Cadence card — the transparency claim about sync frequency */}
      <div className="rounded-[22px] border border-blue/40 bg-blue/[0.04] p-5 sm:p-6 md:p-7 mb-8">
        <div className="flex items-start gap-3 flex-wrap">
          <div className="w-6 h-6 rounded-full bg-blue text-white grid place-items-center font-bold text-[13px] shrink-0">↺</div>
          <div className="flex-1 min-w-[240px]">
            <div className="font-display font-bold text-[18px] sm:text-[20px] tracking-[-0.015em]">
              {t("dataStatus.cadenceTitle")}
            </div>
            <p className="font-serif italic text-[15px] sm:text-[16px] text-ink-2 mt-1 leading-snug">
              {t("dataStatus.intro")}
            </p>
          </div>
          <div className="flex gap-2 flex-wrap">
            {["03:05", "09:05", "15:05", "21:05"].map((tt) => (
              <span key={tt} className="font-mono text-[11px] tracking-[0.14em] bg-white text-ink border border-rule rounded-full px-3 py-1">
                {tt}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Job table */}
      {isError ? (
        <LoadFailed error={error} />
      ) : isPending ? (
        <p className="font-mono text-sm text-muted">{t("common.loading")}</p>
      ) : (!data || data.length === 0) ? (
        <p className="font-serif italic text-muted">{t("dataStatus.empty")}</p>
      ) : (
        <div className="overflow-x-auto no-scrollbar -mx-5 sm:mx-0 px-5 sm:px-0">
          <table className="w-full border border-rule rounded-[20px] overflow-hidden bg-white min-w-[720px]">
            <caption className="sr-only">{t("dataStatus.tableCaption")}</caption>
            <thead className="bg-off">
              <tr className="text-left font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
                <th scope="col" className="px-4 py-3 font-bold">{t("dataStatus.col.source")}</th>
                <th scope="col" className="px-4 py-3 font-bold">{t("dataStatus.col.job")}</th>
                <th scope="col" className="px-4 py-3 font-bold">{t("dataStatus.col.lastRun")}</th>
                <th scope="col" className="px-4 py-3 font-bold">{t("dataStatus.col.status")}</th>
                <th scope="col" className="px-4 py-3 font-bold text-right">{t("dataStatus.col.records")}</th>
              </tr>
            </thead>
            <tbody>
              {data.map((s) => {
                const success = s.lastRunStatus === "SUCCESS";
                return (
                  <tr key={`${s.sourceName}:${s.jobName}`} className="border-t border-rule text-sm">
                    <td className="px-4 py-3 font-mono text-[11px] tracking-[0.06em]">{s.sourceName}</td>
                    <td className="px-4 py-3 font-medium">{s.jobName}</td>
                    <td className="px-4 py-3 font-mono text-[11px] text-muted">
                      {formatDateTime(s.lastRunAt, {
                        day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit",
                      })}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1.5 font-mono text-[10px] tracking-[0.14em] uppercase font-bold ${success ? "text-live-deep" : "text-hot-deep"}`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${success ? "bg-live" : "bg-hot"}`} />
                        {s.lastRunStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-bold">{s.lastRunRecords}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Limitations */}
      <h2 className="font-display font-bold text-[22px] tracking-[-0.02em] mt-10 mb-3">
        {t("dataStatus.limitationsHeading")}
      </h2>
      <ul className="list-disc pl-6 space-y-2 font-serif text-[16px] text-ink-2">
        <li>{t("dataStatus.lim1")}</li>
        <li>{t("dataStatus.lim2")}</li>
        <li>{t("dataStatus.lim3")}</li>
      </ul>

      <div className="mt-10 pt-6 border-t border-rule font-mono text-[10px] tracking-[0.14em] uppercase text-muted flex flex-wrap gap-4">
        <Link to="/methodology" className="text-blue">{t("footer.methodology")} →</Link>
        <Link to="/sources" className="text-blue">{t("footer.sources")} →</Link>
        <Link to="/corrections" className="text-blue">{t("footer.corrections")} →</Link>
      </div>
    </div>
  );
}

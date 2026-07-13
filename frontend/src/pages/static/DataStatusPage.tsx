import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../../api/politicians";

export default function DataStatusPage() {
  const { t } = useTranslation();
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  return (
    <article className="prose max-w-none text-slate-700">
      <h1 className="text-3xl font-semibold text-ink">{t("dataStatus.title")}</h1>
      <p>{t("dataStatus.intro")}</p>
      {(!data || data.length === 0) ? (
        <p className="text-slate-500">{t("dataStatus.empty")}</p>
      ) : (
        <table className="table-auto border border-slate-200">
          <caption className="sr-only">Latest ingestion job runs</caption>
          <thead>
            <tr className="bg-slate-50 text-left text-sm">
              <th scope="col" className="px-3 py-2">{t("dataStatus.col.source")}</th>
              <th scope="col" className="px-3 py-2">{t("dataStatus.col.job")}</th>
              <th scope="col" className="px-3 py-2">{t("dataStatus.col.lastRun")}</th>
              <th scope="col" className="px-3 py-2">{t("dataStatus.col.status")}</th>
              <th scope="col" className="px-3 py-2 text-right">{t("dataStatus.col.records")}</th>
            </tr>
          </thead>
          <tbody>
            {data.map((s) => (
              <tr key={`${s.sourceName}:${s.jobName}`} className="border-t border-slate-200 text-sm">
                <td className="px-3 py-2">{s.sourceName}</td>
                <td className="px-3 py-2">{s.jobName}</td>
                <td className="px-3 py-2">{s.lastRunAt ? new Date(s.lastRunAt).toLocaleString() : "—"}</td>
                <td className="px-3 py-2">{s.lastRunStatus}</td>
                <td className="px-3 py-2 text-right">{s.lastRunRecords}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <h2>{t("dataStatus.limitationsHeading")}</h2>
      <ul>
        <li>{t("dataStatus.lim1")}</li>
        <li>{t("dataStatus.lim2")}</li>
        <li>{t("dataStatus.lim3")}</li>
      </ul>
    </article>
  );
}

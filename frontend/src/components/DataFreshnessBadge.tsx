import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../api/politicians";

/**
 * Freshness for a specific dataset. `job` selects which import's status to show, so a page
 * displays its own source's last sync and the matching cadence (members/legislation = daily,
 * votes = every 6 h) rather than one shared row with a mismatched cadence label.
 */
export default function DataFreshnessBadge({ job }: { job: string }) {
  const { t, i18n } = useTranslation();
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const row = (data ?? []).find((d) => d.jobName === job) ?? data?.[0];
  if (!row?.lastRunAt) return null;
  const when = new Date(row.lastRunAt).toLocaleString(i18n.resolvedLanguage, {
    day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit",
  });
  const cadenceLabel = t(`freshness.cadence.${row.cadence}`, { defaultValue: t("freshness.cadence.GENERIC") });
  return (
    <div className="inline-flex flex-wrap items-center gap-x-2 gap-y-1 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
      <span className="w-1.5 h-1.5 rounded-full bg-live shadow-[0_0_8px_theme(colors.live)] animate-pulse-dot" />
      {t("freshness.lastSync")} <time dateTime={row.lastRunAt} className="text-ink">{when}</time>
      <span className="text-blue">· {row.lastRunStatus}</span>
      <span>· {t("freshness.recordsCount", { count: row.lastRunRecords })}</span>
      <Link to="/data-status" className="text-blue border-b border-blue pb-0.5 hover:opacity-80 normal-case tracking-normal font-sans text-[11px]">
        · {cadenceLabel}
      </Link>
    </div>
  );
}

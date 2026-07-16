import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../api/politicians";

export default function DataFreshnessBadge() {
  const { t, i18n } = useTranslation();
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const first = data?.[0];
  if (!first?.lastRunAt) return null;
  const when = new Date(first.lastRunAt).toLocaleString(i18n.resolvedLanguage, {
    day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit",
  });
  return (
    <div className="inline-flex flex-wrap items-center gap-x-2 gap-y-1 font-mono text-[10px] tracking-[0.14em] uppercase text-muted">
      <span className="w-1.5 h-1.5 rounded-full bg-live shadow-[0_0_8px_theme(colors.live)] animate-pulse-dot" />
      {t("freshness.lastSync")} <time dateTime={first.lastRunAt} className="text-ink">{when}</time>
      <span className="text-blue">· {first.lastRunStatus}</span>
      <span>· {t("freshness.recordsCount", { count: first.lastRunRecords })}</span>
      <Link to="/data-status" className="text-blue border-b border-blue pb-0.5 hover:opacity-80 normal-case tracking-normal font-sans text-[11px]">
        · {t("freshness.cadence")}
      </Link>
    </div>
  );
}

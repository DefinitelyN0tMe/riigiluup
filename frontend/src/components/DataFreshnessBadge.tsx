import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchDataStatus } from "../api/politicians";

export default function DataFreshnessBadge() {
  const { t } = useTranslation();
  const { data } = useQuery({ queryKey: ["data-status"], queryFn: fetchDataStatus });
  const first = data?.[0];
  if (!first?.lastRunAt) return null;
  const when = new Date(first.lastRunAt).toLocaleString();
  return (
    <div className="text-xs text-slate-500">
      {t("freshness.lastSync")} <time dateTime={first.lastRunAt}>{when}</time> — {first.lastRunStatus} ({t("freshness.recordsCount", { count: first.lastRunRecords })})
    </div>
  );
}

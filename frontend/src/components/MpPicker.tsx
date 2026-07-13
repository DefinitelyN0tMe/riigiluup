import { useId, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { fetchPoliticians } from "../api/politicians";

export default function MpPicker({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string | null;
  onChange: (slug: string | null) => void;
}) {
  const { t } = useTranslation();
  const [q, setQ] = useState("");
  const inputId = useId();
  const listboxId = useId();
  const { data } = useQuery({
    queryKey: ["mp-picker", q],
    queryFn: () => fetchPoliticians({ q: q || undefined, size: 8, activeOnly: true }),
    enabled: q.length >= 2,
  });

  const showList = q.length >= 2 && data && data.items.length > 0;
  const resultCount = data?.items.length ?? 0;

  return (
    <div className="flex-1 min-w-[240px]">
      <label htmlFor={inputId} className="block text-sm text-slate-600 mb-1">{label}</label>
      <input
        id={inputId}
        type="search"
        value={q}
        onChange={(e) => { setQ(e.target.value); onChange(null); }}
        placeholder={t("mpPicker.placeholder")}
        aria-controls={showList ? listboxId : undefined}
        aria-expanded={showList ? true : false}
        className="w-full border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
      />
      {q.length >= 2 && (
        <span className="sr-only" aria-live="polite">
          {resultCount === 0 ? "No MPs found." : `${resultCount} matching MPs.`}
        </span>
      )}
      {value && (
        <p className="text-xs text-estonia mt-1">{t("mpPicker.selected", { slug: value })}</p>
      )}
      {showList && (
        <ul id={listboxId} className="mt-1 border border-slate-200 rounded-md divide-y divide-slate-100 max-h-56 overflow-auto bg-white">
          {data!.items.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                onClick={() => { onChange(p.slug); setQ(p.fullName); }}
                className={`block w-full text-left px-3 py-2 text-sm hover:bg-slate-50 ${
                  value === p.slug ? "bg-slate-50 text-estonia" : ""
                }`}
              >
                {p.fullName}
                <span className="text-slate-500"> — {p.factionName ?? t("common.unaffiliated")}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

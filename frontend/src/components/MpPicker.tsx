import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
  const [q, setQ] = useState("");
  const { data } = useQuery({
    queryKey: ["mp-picker", q],
    queryFn: () => fetchPoliticians({ q: q || undefined, size: 8, activeOnly: true }),
    enabled: q.length >= 2,
  });

  return (
    <div className="flex-1 min-w-[240px]">
      <label className="block text-sm text-slate-600 mb-1">{label}</label>
      <input
        type="search"
        value={q}
        onChange={(e) => { setQ(e.target.value); onChange(null); }}
        placeholder="Type MP name…"
        className="w-full border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-estonia"
      />
      {value && (
        <p className="text-xs text-estonia mt-1">Selected: {value}</p>
      )}
      {q.length >= 2 && data && data.items.length > 0 && (
        <ul className="mt-1 border border-slate-200 rounded-md divide-y divide-slate-100 max-h-56 overflow-auto bg-white">
          {data.items.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                onClick={() => { onChange(p.slug); setQ(p.fullName); }}
                className={`block w-full text-left px-3 py-2 text-sm hover:bg-slate-50 ${
                  value === p.slug ? "bg-slate-50 text-estonia" : ""
                }`}
              >
                {p.fullName}
                <span className="text-slate-500"> — {p.factionName ?? "Unaffiliated"}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

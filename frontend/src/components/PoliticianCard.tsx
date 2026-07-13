import { Link } from "react-router-dom";
import type { Politician } from "../types";

export default function PoliticianCard({ p }: { p: Politician }) {
  const initials = `${p.firstName?.[0] ?? ""}${p.lastName?.[0] ?? ""}`.toUpperCase();
  return (
    <Link
      to={`/politicians/${encodeURIComponent(p.slug)}`}
      className="block border border-slate-200 rounded-lg p-4 hover:shadow-sm hover:border-estonia transition"
    >
      <div className="flex items-start gap-3">
        {p.photoUrl ? (
          <img
            src={p.photoUrl}
            alt=""
            loading="lazy"
            className="w-14 h-14 rounded-full object-cover bg-slate-100"
          />
        ) : (
          <div className="w-14 h-14 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 text-sm font-medium">
            {initials || "??"}
          </div>
        )}
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-ink truncate">{p.fullName}</h3>
          <p className="text-sm text-slate-600 truncate">
            {p.factionName ?? "No faction"}
          </p>
        </div>
      </div>
    </Link>
  );
}

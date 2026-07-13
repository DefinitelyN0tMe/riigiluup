import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { Politician } from "../types";
import { resolveMediaUrl } from "../api/client";

export default function PoliticianCard({ p }: { p: Politician }) {
  const { t } = useTranslation();
  const initials = `${p.firstName?.[0] ?? ""}${p.lastName?.[0] ?? ""}`.toUpperCase();
  const photoSrc = resolveMediaUrl(p.photoUrl);
  return (
    <Link
      to={`/politicians/${encodeURIComponent(p.slug)}`}
      className="block border border-slate-200 rounded-lg p-4 hover:shadow-sm hover:border-estonia transition"
    >
      <div className="flex items-start gap-3">
        {photoSrc ? (
          <img
            src={photoSrc}
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
          <h2 className="text-base font-semibold text-ink truncate">{p.fullName}</h2>
          <p className="text-sm text-slate-600 truncate">
            {p.factionName ?? t("common.noFaction")}
          </p>
        </div>
      </div>
    </Link>
  );
}

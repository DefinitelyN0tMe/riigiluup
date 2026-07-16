import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { Politician } from "../types";
import { resolveMediaUrl } from "../api/client";

const PARTY_COLOR: Record<string, string> = {
  "Eesti Reformierakonna fraktsioon": "#0072CE",
  "Eesti Keskerakonna fraktsioon": "#003E7E",
  "Eesti Konservatiivse Rahvaerakonna fraktsioon": "#0A0A0A",
  "Isamaa fraktsioon": "#FFB020",
  "Sotsiaaldemokraatliku Erakonna fraktsioon": "#FF4B3E",
  "Eesti 200 fraktsioon": "#1EA98A",
};

export default function PoliticianCard({
  p,
  compareActive,
  compareDisabled,
  onToggleCompare,
}: {
  p: Politician;
  compareActive?: boolean;
  compareDisabled?: boolean;
  onToggleCompare?: (slug: string) => void;
}) {
  const { t } = useTranslation();
  const initials = `${p.firstName?.[0] ?? ""}${p.lastName?.[0] ?? ""}`.toUpperCase();
  const photoSrc = resolveMediaUrl(p.photoUrl);
  const color = PARTY_COLOR[p.factionName ?? ""] ?? "#0072CE";
  const canToggle = onToggleCompare && (!compareDisabled || compareActive);
  return (
    <Link
      to={`/politicians/${encodeURIComponent(p.slug)}`}
      className="block bg-white border border-rule rounded-[22px] p-5 hover:-translate-y-0.5 hover:border-blue transition-all relative group"
    >
      {onToggleCompare && (
        <button
          type="button"
          aria-pressed={compareActive}
          aria-label={compareActive ? t("compare.removeFromTray") : t("compare.addToTray")}
          title={compareActive ? t("compare.removeFromTray") : t("compare.addToTray")}
          disabled={!canToggle}
          onClick={(e) => { e.preventDefault(); e.stopPropagation(); onToggleCompare(p.slug); }}
          className={`absolute top-3 left-3 w-7 h-7 rounded-full grid place-items-center text-[13px] font-bold border transition-all
            ${compareActive
              ? "bg-blue text-white border-blue shadow-sm"
              : "bg-white/85 text-ink border-rule opacity-0 group-hover:opacity-100 focus:opacity-100 hover:border-blue hover:text-blue"}
            ${!canToggle ? "cursor-not-allowed opacity-30" : ""}`}
        >
          {compareActive ? "✓" : "+"}
        </button>
      )}
      <div className="flex justify-between items-start mb-5">
        <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted min-w-0 truncate pr-3 pl-8">
          {p.factionName?.replace(/fraktsioon/i, "").trim() ?? t("common.noFaction")}
        </div>
        {photoSrc ? (
          <img src={photoSrc} alt="" loading="lazy" className="w-11 h-11 rounded-full object-cover shrink-0" />
        ) : (
          <div className="w-11 h-11 rounded-full grid place-items-center font-bold text-[14px] shrink-0 text-white"
               style={{ backgroundColor: color }}>
            {initials || "??"}
          </div>
        )}
      </div>
      <h2 className="font-display font-bold text-[20px] leading-[1.1] tracking-[-0.025em]">
        {p.fullName}
      </h2>
      <p className="text-[12px] mt-1 text-muted truncate">
        {p.factionName ?? t("common.noFaction")}
      </p>
    </Link>
  );
}

import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { Politician } from "../types";
import { resolveMediaUrl } from "../api/client";
import { partyColor } from "../lib/partyColors";

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
  const color = partyColor(p.factionName);
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
              : "bg-white text-blue border-blue/50 shadow-sm hover:bg-blue hover:text-white hover:border-blue"}
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
          <img src={photoSrc} alt={p.fullName} loading="lazy" className="w-11 h-11 rounded-full object-cover shrink-0" />
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

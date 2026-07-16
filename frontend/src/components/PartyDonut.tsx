import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

type Props = {
  seats: number;
  totalSeats?: number;
  color?: string;
  label: string;
  side: "coalition" | "opposition";
  factionExternalId?: string;   // enables click-through to filtered /politicians
};

export default function PartyDonut({
  seats, totalSeats = 101, color = "#0072CE", label, side, factionExternalId,
}: Props) {
  const { t } = useTranslation();
  const dash = (seats / totalSeats) * 100;
  const Wrapper: React.ElementType = factionExternalId ? Link : "div";
  const wrapperProps = factionExternalId
    ? { to: `/politicians?faction=${encodeURIComponent(factionExternalId)}` }
    : {};
  return (
    <Wrapper {...wrapperProps} className="bg-white border border-rule rounded-[20px] p-5 sm:p-6 flex flex-col items-center gap-4 hover:border-blue hover:-translate-y-0.5 transition-all">
      <svg viewBox="0 0 42 42" className="w-24 h-24 sm:w-28 sm:h-28">
        <circle cx="21" cy="21" r="15.915" fill="none" stroke="#F1F0EA" strokeWidth="3.5" />
        <circle cx="21" cy="21" r="15.915" fill="none" stroke={color} strokeWidth="3.5"
                strokeDasharray={`${dash} 100`} strokeDashoffset="25"
                transform="rotate(-90 21 21)" />
        <text x="21" y="21" textAnchor="middle" dominantBaseline="central"
              fontFamily="Bricolage Grotesque" fontWeight="700" fontSize="10"
              fill="#0A0A0A" letterSpacing="-0.05em">{seats}</text>
      </svg>
      <div className="text-center">
        <h4 className="font-display font-bold text-[15px] sm:text-[17px] tracking-[-0.01em] leading-tight">{label}</h4>
        <div className="font-mono text-[10px] tracking-[0.14em] uppercase text-muted mt-1">
          <b className="text-ink font-bold">{seats}</b> {t("partyDonut.mandate")} · {side === "coalition" ? t("partyDonut.coalition") : t("partyDonut.opposition")}
        </div>
      </div>
    </Wrapper>
  );
}

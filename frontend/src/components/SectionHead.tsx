import type { ReactNode } from "react";
import { Link } from "react-router-dom";

type Props = {
  index: string;   // "I.", "II." etc
  kicker: string;
  title: ReactNode;
  more?: { to: string; label: string };
  onInk?: boolean;
};

export default function SectionHead({ index, kicker, title, more, onInk = false }: Props) {
  const kickerCls = onInk ? "text-blue-glow" : "text-blue";
  const ruleCls = onInk ? "border-white/20" : "border-ink";
  const moreCls = onInk
    ? "border-white text-white hover:bg-white hover:text-ink"
    : "border-ink text-ink hover:bg-ink hover:text-white";
  return (
    <div className="grid grid-cols-1 md:grid-cols-[auto_1fr_auto] gap-6 md:gap-8 md:items-end mb-8 md:mb-14">
      <div>
        <div className={`font-mono text-[11px] tracking-[0.2em] uppercase font-bold ${kickerCls} flex items-center gap-2.5`}>
          <span className={`${onInk ? "bg-blue-glow text-ink" : "bg-blue text-white"} px-2 py-0.5 rounded font-bold tracking-[0.14em]`}>{index}</span>
          {kicker}
        </div>
        <h2 className="mt-4 font-display font-bold h-display-lg">{title}</h2>
      </div>
      <div className={`hidden md:block border-t-2 ${ruleCls} self-end mb-4`} />
      {more && (
        <Link to={more.to}
           className={`justify-self-start md:justify-self-end inline-flex items-center gap-2.5 px-4 sm:px-5 py-2.5 sm:py-3 border-2 rounded-full font-bold text-[13px] sm:text-[14px] tracking-[-0.005em] transition-colors ${moreCls}`}>
          {more.label}
          <span className="w-5 h-5 rounded-full bg-ink text-white grid place-items-center font-mono text-[11px] rotate-[-45deg]">↑</span>
        </Link>
      )}
    </div>
  );
}

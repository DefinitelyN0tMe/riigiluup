type Variant = "light" | "dark";

export default function BrandLogo({ variant = "dark", showTag = true }: { variant?: Variant; showTag?: boolean }) {
  const isLight = variant === "light";
  return (
    <span className={`inline-flex items-center gap-2.5 font-display font-bold text-[22px] tracking-[-0.02em] ${isLight ? "text-white" : "text-ink"}`}>
      {/* Thin frame so the blue top stripe stays visible against a blue background
          (e.g. the homepage hero); adapts to light/dark surfaces. */}
      <span className={`shrink-0 rounded-[3px] overflow-hidden leading-none ${isLight ? "ring-1 ring-white/50" : "ring-1 ring-black/10"}`}>
        <svg viewBox="0 0 30 20" className="w-[30px] h-[20px] block" aria-hidden>
          <rect y="0"     width="30" height="6.67" fill="#0072CE" />
          <rect y="6.67"  width="30" height="6.66" fill="#0A0A0A" />
          <rect y="13.33" width="30" height="6.67" fill={isLight ? "#F4F4F1" : "#FFFFFF"} />
        </svg>
      </span>
      Riigiluup
      {showTag && (
        <span className={`font-mono text-[10px] tracking-[0.18em] uppercase font-medium ${isLight ? "text-white/70" : "text-muted"} ml-1`}>
          .ee
        </span>
      )}
    </span>
  );
}

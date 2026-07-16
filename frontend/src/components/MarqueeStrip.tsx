import { useTranslation } from "react-i18next";

export default function MarqueeStrip() {
  const { t } = useTranslation();
  const WORDS = [
    t("chrome.marquee.w1"),
    t("chrome.marquee.w2"),
    t("chrome.marquee.w3"),
    t("chrome.marquee.w4"),
    t("chrome.marquee.w5"),
    t("chrome.marquee.w6"),
  ];
  const doubled = [...WORDS, ...WORDS, ...WORDS, ...WORDS];
  return (
    <div className="bg-blue text-white overflow-hidden border-y border-white/10 relative">
      <div className="marquee-row animate-scroll-x-fast items-center py-3.5 sm:py-5 md:py-6">
        {doubled.map((w, i) => (
          <span key={i} className="inline-flex items-center gap-6 sm:gap-11">
            {/^[a-zäõöа-я]/i.test(w) && w.endsWith(".") ? (
              <span className="font-serif italic font-light text-stroke-white text-[30px] xs:text-[36px] sm:text-[44px] md:text-[52px] lg:text-[56px] leading-none">{w}</span>
            ) : (
              <span className="font-display font-bold text-[30px] xs:text-[36px] sm:text-[44px] md:text-[52px] lg:text-[56px] tracking-tightest leading-none">{w}</span>
            )}
            <span className="w-3 h-3 sm:w-4 sm:h-4 md:w-5 md:h-5 rounded-full bg-blue-glow shrink-0 shadow-[0_0_24px_theme(colors.blue.glow)]" />
          </span>
        ))}
      </div>
    </div>
  );
}

import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import BrandLogo from "./BrandLogo";

export default function ManifestoFooter() {
  const { t } = useTranslation();
  return (
    <footer className="bg-ink text-white relative overflow-hidden px-5 sm:px-8 md:px-10 pt-12 sm:pt-16 md:pt-20 pb-10">
      <div className="grain" />
      {/* Manifesto */}
      <div className="relative font-display font-extrabold h-display-xl leading-[0.95]">
        <span className="block">{t("chrome.manifesto.l1")}</span>
        <span className="block font-serif italic font-light text-stroke-white">{t("chrome.manifesto.l2")}</span>
        <span className="block">
          <span className="inline-block align-middle w-16 sm:w-24 h-1 bg-blue mr-4 sm:mr-6 mb-4 sm:mb-6" />
          {t("chrome.manifesto.l3")}
        </span>
      </div>

      {/* Foot columns */}
      <div className="relative mt-10 sm:mt-14 pt-8 sm:pt-10 border-t border-white/[0.14] grid grid-cols-2 md:grid-cols-4 gap-8 md:gap-10">
        <div className="col-span-2 md:col-span-1">
          <BrandLogo variant="light" />
          <p className="mt-4 text-sm opacity-60 leading-relaxed max-w-[34ch]">
            {t("chrome.footerBlurb")}
          </p>
        </div>
        <div>
          <h5 className="font-mono text-[10px] tracking-[0.2em] uppercase text-blue-glow font-bold mb-2">{t("chrome.foot.browse")}</h5>
          <p className="text-xs opacity-45 leading-snug mb-4 max-w-[28ch]">{t("chrome.foot.browseDesc")}</p>
          <ul className="space-y-2 text-sm">
            <li><Link to="/politicians" className="opacity-80 hover:opacity-100">{t("nav.mps")}</Link></li>
            <li><Link to="/votes" className="opacity-80 hover:opacity-100">{t("nav.votes")}</Link></li>
            <li><Link to="/legislation" className="opacity-80 hover:opacity-100">{t("nav.bills")}</Link></li>
            <li><Link to="/analytics" className="opacity-80 hover:opacity-100">{t("nav.analytics")}</Link></li>
            <li><Link to="/compare" className="opacity-80 hover:opacity-100">{t("nav.compare")}</Link></li>
          </ul>
        </div>
        <div>
          <h5 className="font-mono text-[10px] tracking-[0.2em] uppercase text-blue-glow font-bold mb-2">{t("chrome.foot.transparency")}</h5>
          <p className="text-xs opacity-45 leading-snug mb-4 max-w-[28ch]">{t("chrome.foot.transparencyDesc")}</p>
          <ul className="space-y-2 text-sm">
            <li><Link to="/methodology" className="opacity-80 hover:opacity-100">{t("footer.methodology")}</Link></li>
            <li><Link to="/sources" className="opacity-80 hover:opacity-100">{t("footer.sources")}</Link></li>
            <li><Link to="/data-status" className="opacity-80 hover:opacity-100">{t("footer.dataFreshness")}</Link></li>
            <li><Link to="/corrections" className="opacity-80 hover:opacity-100">{t("footer.corrections")}</Link></li>
          </ul>
        </div>
        <div>
          <h5 className="font-mono text-[10px] tracking-[0.2em] uppercase text-blue-glow font-bold mb-2">{t("chrome.foot.citizen")}</h5>
          <p className="text-xs opacity-45 leading-snug mb-4 max-w-[28ch]">{t("chrome.foot.citizenDesc")}</p>
          <ul className="space-y-2 text-sm">
            <li><Link to="/about" className="opacity-80 hover:opacity-100">{t("footer.about")}</Link></li>
            <li><Link to="/privacy" className="opacity-80 hover:opacity-100">{t("footer.privacy")}</Link></li>
            <li><Link to="/terms" className="opacity-80 hover:opacity-100">{t("footer.terms")}</Link></li>
            <li><a href="https://api.riigikogu.ee" target="_blank" rel="noreferrer noopener" className="opacity-80 hover:opacity-100">{t("chrome.foot.riigikoguApi")}</a></li>
          </ul>
        </div>
      </div>

      <div className="relative mt-10 pt-6 border-t border-white/[0.14] font-mono text-[10px] tracking-[0.14em] uppercase text-white/55 flex flex-col sm:flex-row gap-2 sm:justify-between">
        <span>© 2026 RIIGILUUP.EE · CC BY-SA 3.0</span>
        <span>ANDMED © RIIGIKOGU</span>
      </div>
    </footer>
  );
}

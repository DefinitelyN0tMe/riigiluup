import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import BrandLogo from "./BrandLogo";
import LiveTicker from "./LiveTicker";
import ManifestoFooter from "./ManifestoFooter";
import LocaleSwitcher from "./LocaleSwitcher";
import ErrorBoundary from "./ErrorBoundary";
import ScrollToTopButton from "./ScrollToTopButton";

/**
 * Site chrome. Nav is themed: transparent-on-blue on the home page (where hero is blue),
 * off-white with black text everywhere else. Mobile: hamburger opens a full-height sheet.
 */
export default function Layout() {
  const { t } = useTranslation();
  const location = useLocation();
  const isHome = location.pathname === "/";
  const [menuOpen, setMenuOpen] = useState(false);
  const cursorRef = useRef<HTMLDivElement>(null);

  // Close mobile menu + jump to top of page on route change
  useEffect(() => {
    setMenuOpen(false);
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname]);

  // Custom cursor (desktop, hover-capable only — CSS hides it on touch)
  useEffect(() => {
    const el = cursorRef.current;
    if (!el) return;
    let raf = 0;
    let x = -100, y = -100;
    const move = (e: MouseEvent) => {
      x = e.clientX; y = e.clientY;
      if (!raf) raf = requestAnimationFrame(apply);
    };
    const apply = () => {
      if (el) el.style.transform = `translate3d(${x}px, ${y}px, 0) translate(-50%, -50%)`;
      raf = 0;
    };
    window.addEventListener("mousemove", move, { passive: true });
    return () => { window.removeEventListener("mousemove", move); cancelAnimationFrame(raf); };
  }, []);

  const navItems = [
    { to: "/", label: t("nav.home"), end: true },
    { to: "/politicians", label: t("nav.mps"), end: false },
    { to: "/votes", label: t("nav.votes"), end: false },
    { to: "/legislation", label: t("nav.bills"), end: false },
    { to: "/speeches", label: t("nav.speeches"), end: false },
    { to: "/analytics", label: t("nav.analytics"), end: false },
    { to: "/methodology", label: t("footer.methodology"), end: false },
  ];

  const navTheme = isHome
    ? "bg-blue text-white"
    : "bg-paper text-ink border-b border-rule";

  const linkBase = "px-3.5 py-2 text-[13px] font-medium tracking-[-0.005em] rounded-full transition-colors";
  const linkOn   = isHome ? "bg-white text-blue font-bold" : "bg-ink text-white font-bold";
  const linkOff  = isHome ? "text-white hover:bg-white/12" : "text-ink hover:bg-black/[0.06]";

  return (
    <div className="min-h-screen flex flex-col bg-off">
      <a href="#main" className="skip-link">{t("a11y.skipToContent")}</a>
      <div ref={cursorRef} className="cursor-dot" aria-hidden>
        <svg viewBox="0 0 34 34" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <circle cx="17" cy="17" r="9" />
          <line x1="23.5" y1="23.5" x2="32" y2="32" />
        </svg>
      </div>

      {/* LIVE TICKER (top) */}
      <LiveTicker />

      {/* NAV */}
      <div className={navTheme}>
        <nav className="grid grid-cols-[auto_1fr_auto] items-center px-4 sm:px-6 md:px-10 py-4 gap-4 md:gap-8">
          <Link to="/" aria-label={t("nav.brand")} className="shrink-0">
            <BrandLogo variant={isHome ? "light" : "dark"} showTag={true} />
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex justify-center gap-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end}
                className={({ isActive }) => `${linkBase} ${isActive ? linkOn : linkOff}`}>
                {item.label}
              </NavLink>
            ))}
          </div>

          {/* Right side */}
          <div className="flex items-center gap-2 sm:gap-4 justify-end">
            <LocaleSwitcher variant={isHome ? "light" : "dark"} />
            {/* Mobile hamburger */}
            <button
              type="button"
              className={`md:hidden inline-flex items-center justify-center w-10 h-10 rounded-full border ${isHome ? "border-white/40 text-white" : "border-ink/30 text-ink"}`}
              aria-label={menuOpen ? t("chrome.menuClose") : t("chrome.menuOpen")}
              aria-expanded={menuOpen}
              aria-controls="mobile-menu"
              onClick={() => setMenuOpen((v) => !v)}
            >
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                {menuOpen ? (
                  <><line x1="5" y1="5" x2="19" y2="19"/><line x1="19" y1="5" x2="5" y2="19"/></>
                ) : (
                  <><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></>
                )}
              </svg>
            </button>
          </div>
        </nav>

        {/* Mobile menu sheet */}
        {menuOpen && (
          <div id="mobile-menu" className={`md:hidden px-4 pb-4 ${isHome ? "" : ""}`}>
            <ul className="flex flex-col gap-1 pt-2">
              {navItems.map((item) => (
                <li key={item.to}>
                  <NavLink to={item.to} end={item.end}
                    className={({ isActive }) =>
                      `block px-4 py-3 rounded-2xl text-[15px] font-semibold ${
                        isActive
                          ? isHome ? "bg-white text-blue" : "bg-ink text-white"
                          : isHome ? "bg-white/10 text-white" : "bg-white text-ink border border-rule"
                      }`
                    }>
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Main */}
      <main id="main" tabIndex={-1} className="flex-1 outline-none">
        <ErrorBoundary key={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>

      {/* Footer */}
      <ManifestoFooter />

      <ScrollToTopButton />
    </div>
  );
}

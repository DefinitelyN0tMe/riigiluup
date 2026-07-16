import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

/**
 * Floating "back to top" button, bottom-right. Appears once the page is scrolled
 * past ~one viewport and smoothly returns to the top on click. Rendered once in
 * the layout, so it's available on every page.
 */
export default function ScrollToTopButton() {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > 600);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  if (!visible) return null;

  return (
    <button
      type="button"
      onClick={() => window.scrollTo({ top: 0, left: 0, behavior: "smooth" })}
      aria-label={t("a11y.backToTop")}
      title={t("a11y.backToTop")}
      className="fixed bottom-5 right-5 sm:bottom-6 sm:right-6 z-40 w-11 h-11 sm:w-12 sm:h-12 rounded-full bg-ink text-white grid place-items-center shadow-lg shadow-black/25 hover:bg-blue transition-colors focus:outline-none focus:ring-2 focus:ring-blue-glow focus:ring-offset-2 focus:ring-offset-off"
    >
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor"
           strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        <path d="M12 19V6M6 12l6-6 6 6" />
      </svg>
    </button>
  );
}

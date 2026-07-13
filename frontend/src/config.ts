/** Central place for site-wide constants. Values read from env at build time. */
export const CORRECTIONS_EMAIL =
    (import.meta.env.VITE_CORRECTIONS_EMAIL as string | undefined)
    ?? "corrections@politico.example";

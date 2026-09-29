const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export function resolveMediaUrl(path: string | null | undefined): string | undefined {
  if (!path) return undefined;
  if (/^https?:\/\//i.test(path)) return path;
  return `${BASE}${path}`;
}

/**
 * Structured API error. UIs can inspect {@link status} and use {@link i18nKey}
 * to render a translated message instead of the raw HTTP text.
 * `apiErrors.<key>` is expected to exist in every locale (see i18n).
 */
export class ApiError extends Error {
  status: number;
  path: string;
  i18nKey: string;
  /** Seconds the server asked us to wait (Retry-After on a 429), if it said. */
  retryAfter: number | null;
  constructor(status: number, path: string, statusText: string, retryAfter: number | null = null) {
    super(`API ${status} ${statusText} for ${path}`);
    this.name = "ApiError";
    this.status = status;
    this.path = path;
    this.i18nKey = pickI18nKey(status);
    this.retryAfter = retryAfter;
  }
}

function pickI18nKey(status: number): string {
  if (status === 401 || status === 403) return "apiErrors.unauthorized";
  if (status === 404) return "apiErrors.notFound";
  if (status === 429) return "apiErrors.rateLimited";
  if (status === 400 || status === 422) return "apiErrors.badRequest";
  if (status >= 500) return "apiErrors.serverError";
  return "apiErrors.generic";
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers: { Accept: "application/json" } });
  if (!res.ok) {
    const ra = Number(res.headers.get("Retry-After"));
    throw new ApiError(res.status, path, res.statusText, Number.isFinite(ra) && ra > 0 ? ra : null);
  }
  return res.json() as Promise<T>;
}

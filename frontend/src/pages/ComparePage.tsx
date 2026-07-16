import { Navigate, useSearchParams } from "react-router-dom";

/**
 * Compare functionality moved inline onto /politicians. This route now acts as a
 * permanent redirect; kept so existing bookmarks/external links keep working.
 * Forwards any legacy ?left=&right= query params to the new tray-style state
 * on /politicians (?compareLeft=&compareRight=).
 */
export default function ComparePage() {
  const [sp] = useSearchParams();
  const next = new URLSearchParams();
  const legacyLeft = sp.get("left") ?? sp.get("compareLeft");
  const legacyRight = sp.get("right") ?? sp.get("compareRight");
  if (legacyLeft) next.set("compareLeft", legacyLeft);
  if (legacyRight) next.set("compareRight", legacyRight);
  const search = next.toString();
  return <Navigate to={`/politicians${search ? "?" + search : ""}`} replace />;
}

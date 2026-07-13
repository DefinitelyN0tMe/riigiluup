import type { Faction, Party } from "../types";

export default function FactionBadge({
  faction, party,
}: {
  faction: Faction | null;
  party: Party | null;
}) {
  if (!faction) return <span className="text-slate-500">No faction</span>;
  const color = party?.colorHex ?? "#0072ce";
  return (
    <div className="inline-flex items-center gap-2">
      <span
        className="inline-block w-3 h-3 rounded-full"
        style={{ backgroundColor: color }}
        aria-hidden
      />
      <span className="font-medium">{faction.name}</span>
      {party && (
        <span className="text-slate-500 text-sm">— {party.shortName}</span>
      )}
    </div>
  );
}

type Props = {
  points: number[];   // 0..1 values or arbitrary; we'll normalize
  color?: string;
  filled?: boolean;
  height?: number;
  width?: number;
  className?: string;
};

export default function Sparkline({
  points, color = "#0072CE", filled = false, height = 26, width = 200, className = "",
}: Props) {
  if (!points.length) return null;
  const min = Math.min(...points);
  const max = Math.max(...points);
  const range = Math.max(max - min, 1e-6);
  const step = width / Math.max(points.length - 1, 1);
  const pts = points.map((p, i) => `${(i * step).toFixed(1)},${(height - ((p - min) / range) * (height - 2) - 1).toFixed(1)}`).join(" ");
  const area = `0,${height} ${pts} ${width},${height}`;
  return (
    <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className={`w-full ${className}`} aria-hidden>
      {filled && <polyline points={area} fill={color} fillOpacity="0.2" stroke="none" />}
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}

type MetricBarProps = {
  label: string;
  value: number | string | null | undefined;
  unit: string;
  min: number;
  max: number;
  signed?: boolean;
};

function toNumber(value: MetricBarProps["value"]) {
  if (value === null || value === undefined || value === "") return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function MetricBar({
  label,
  value,
  unit,
  min,
  max,
  signed = false,
}: MetricBarProps) {
  const numericValue = toNumber(value);
  const range = max - min;
  const position = numericValue === null ? 0 : ((numericValue - min) / range) * 100;
  const clampedPosition = Math.max(0, Math.min(100, position));
  const zeroPosition = signed ? Math.max(0, Math.min(100, ((0 - min) / range) * 100)) : 0;
  const barLeft = signed ? Math.min(zeroPosition, clampedPosition) : 0;
  const barWidth = signed
    ? Math.abs(clampedPosition - zeroPosition)
    : clampedPosition;

  return (
    <div className="monitor-metric">
      <div className="monitor-metric__topline">
        <span>{label}</span>
        <span className="monitor-metric__reading">
          {numericValue === null ? "—" : numericValue}
          <small>{unit}</small>
        </span>
      </div>
      <div className="monitor-metric__track" aria-hidden="true">
        {signed && (
          <span
            className="monitor-metric__zero"
            style={{ left: `${zeroPosition}%` }}
          />
        )}
        {numericValue !== null && (
          <span
            className="monitor-metric__fill"
            style={{ left: `${barLeft}%`, width: `${barWidth}%` }}
          />
        )}
      </div>
    </div>
  );
}

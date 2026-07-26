import React from 'react';

const BarGroup = ({ label, value, max, color }) => {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-3 group">
      <span className="w-20 text-xs text-gray-500 text-right shrink-0 truncate" title={label}>{label}</span>
      <div className="flex-1 bg-gray-100 rounded-full h-3 overflow-hidden">
        <div
          className={`h-3 rounded-full transition-all duration-700 ease-out ${color}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="w-10 text-xs font-semibold text-gray-700 text-right">{value?.toLocaleString()}</span>
    </div>
  );
};

export default BarGroup;

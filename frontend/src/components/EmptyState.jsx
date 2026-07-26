import React from 'react';
import { Database } from 'lucide-react';

const EmptyState = ({ message = 'Sin registros' }) => (
  <div className="flex flex-col items-center justify-center py-16 text-gray-400">
    <Database className="w-12 h-12 mb-3 opacity-40" />
    <p className="text-sm">{message}</p>
  </div>
);

export default EmptyState;

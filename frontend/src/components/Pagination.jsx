import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const Pagination = ({ page, totalPages, onPage }) => (
  <div className="flex items-center justify-between px-6 py-3 border-t border-gray-100 bg-gray-50">
    <span className="text-sm text-gray-500">Página {page + 1} de {Math.max(totalPages, 1)}</span>
    <div className="flex gap-2">
      <button
        onClick={() => onPage(page - 1)}
        disabled={page === 0}
        className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-white transition-colors"
      >
        <ChevronLeft className="w-4 h-4" />
      </button>
      <button
        onClick={() => onPage(page + 1)}
        disabled={page + 1 >= totalPages}
        className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-white transition-colors"
      >
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  </div>
);

export default Pagination;

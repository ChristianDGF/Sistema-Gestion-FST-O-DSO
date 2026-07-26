import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { downloadFile } from '../utils/download';
import {
  BarChart3, Package, DollarSign, AlertTriangle, Download,
  Calendar, ArrowDownToLine, ArrowUpFromLine, Activity, AlertCircle,
} from 'lucide-react';
import BarGroup from '../components/charts/BarGroup';
import EmptyState from '../components/EmptyState';

const Spinner = () => (
  <div className="flex items-center justify-center py-16">
    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
  </div>
);

const StatCard = ({ icon: Icon, label, value, color }) => (
  <div className={`card p-5 flex items-center gap-4 border-l-4 ${color}`}>
    <div className="p-3 rounded-xl bg-gray-50">
      <Icon className="w-6 h-6 text-gray-600" />
    </div>
    <div>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
    </div>
  </div>
);

const formatCurrency = (value) =>
  `$${Number(value ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

// ─────────────────────────────────────────────────────────────
// Pestaña: Valuación de Inventario
// ─────────────────────────────────────────────────────────────
const InventoryValuationTab = () => {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const { data } = await api.get('/reports/inventory-valuation');
        setReport(data);
      } catch (e) {
        console.error('Failed to fetch inventory valuation report', e);
        setError('No se pudo cargar el reporte de valuación de inventario.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleDownload = async () => {
    try {
      setDownloading(true);
      await downloadFile('/reports/inventory-valuation/export', {}, 'valuacion-inventario.csv');
    } catch (e) {
      console.error('Failed to download inventory valuation CSV', e);
      alert('No se pudo descargar el CSV.');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <Spinner />;
  if (error) return (
    <div className="flex items-center gap-2 text-red-500 p-6">
      <AlertCircle className="w-5 h-5" /> {error}
    </div>
  );

  const byCategory = report?.byCategory ?? [];
  const maxValue = Math.max(...byCategory.map((c) => Number(c.totalValue) || 0), 1);

  return (
    <div className="space-y-6 animate-fadeIn">
      <div className="flex justify-end">
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="btn-primary flex items-center"
        >
          <Download className="w-4 h-4 mr-2" />
          {downloading ? 'Descargando...' : 'Descargar CSV'}
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={DollarSign} label="Valor Total"
          value={formatCurrency(report?.totalInventoryValue)} color="border-emerald-500" />
        <StatCard icon={Package} label="Total Productos" value={report?.totalProducts ?? 0} color="border-blue-500" />
        <StatCard icon={Package} label="Productos Activos" value={report?.activeProducts ?? 0} color="border-primary-500" />
        <StatCard icon={AlertTriangle} label="Bajo Stock Mínimo" value={report?.lowStockCount ?? 0} color="border-amber-500" />
      </div>

      <div className="card p-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-5 flex items-center gap-2">
          <BarChart3 className="w-4 h-4 text-primary-600" /> Valor de inventario por categoría
        </h3>
        {byCategory.length === 0 ? (
          <EmptyState message="No hay productos registrados" />
        ) : (
          <div className="space-y-3">
            {byCategory.map((cat) => (
              <BarGroup
                key={cat.category}
                label={cat.category}
                value={Number(cat.totalValue)}
                max={maxValue}
                color="bg-emerald-500"
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Pestaña: Movimientos de Stock por rango de fechas
// ─────────────────────────────────────────────────────────────
const MOVEMENT_ICON = { IN: ArrowDownToLine, OUT: ArrowUpFromLine, ADJUSTMENT: Activity };

const today = () => new Date().toISOString().slice(0, 10);
const daysAgo = (n) => new Date(Date.now() - n * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

const StockMovementsTab = () => {
  const [startDate, setStartDate] = useState(daysAgo(30));
  const [endDate, setEndDate] = useState(today());
  const [category, setCategory] = useState('');
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [downloading, setDownloading] = useState(false);

  const generateReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const { data } = await api.get('/reports/stock-movements', {
        params: { startDate, endDate, category: category || undefined },
      });
      setReport(data);
    } catch (e) {
      console.error('Failed to generate stock movement report', e);
      setError('No se pudo generar el reporte de movimientos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    generateReport();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDownload = async () => {
    try {
      setDownloading(true);
      await downloadFile('/reports/stock-movements/export',
        { startDate, endDate, category: category || undefined }, 'movimientos-stock.csv');
    } catch (e) {
      console.error('Failed to download stock movement CSV', e);
      alert('No se pudo descargar el CSV.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <div className="card p-4 flex flex-col sm:flex-row items-start sm:items-end gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Desde</label>
          <input type="date" className="input-field" value={startDate}
            onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Hasta</label>
          <input type="date" className="input-field" value={endDate}
            onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Categoría (opcional)</label>
          <input type="text" className="input-field" placeholder="Todas" value={category}
            onChange={(e) => setCategory(e.target.value)} />
        </div>
        <button onClick={generateReport} className="btn-primary flex items-center">
          <Calendar className="w-4 h-4 mr-2" />
          Generar reporte
        </button>
        {report && (
          <button
            onClick={handleDownload}
            disabled={downloading}
            className="flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <Download className="w-4 h-4 mr-2" />
            {downloading ? 'Descargando...' : 'Descargar CSV'}
          </button>
        )}
      </div>

      {loading ? (
        <Spinner />
      ) : error ? (
        <div className="flex items-center gap-2 text-red-500 p-6">
          <AlertCircle className="w-5 h-5" /> {error}
        </div>
      ) : report && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard icon={Activity} label="Total Movimientos" value={report.totalMovements} color="border-primary-500" />
            {report.byType.map((t) => (
              <StatCard key={t.movementType} icon={MOVEMENT_ICON[t.movementType] || Activity}
                label={`${t.movementType} (${t.count})`} value={t.totalQuantity} color="border-blue-500" />
            ))}
          </div>

          <div className="card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Producto</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cantidad</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Usuario</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {report.movements.length === 0 ? (
                    <tr>
                      <td colSpan="5">
                        <EmptyState message="No hay movimientos en el rango seleccionado" />
                      </td>
                    </tr>
                  ) : (
                    report.movements.map((m) => (
                      <tr key={m.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {new Date(m.createdAt).toLocaleString()}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{m.productName}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{m.movementType}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {m.movementType === 'OUT' ? '-' : '+'}{m.quantity}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{m.userId}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Página principal — Reportes
// ─────────────────────────────────────────────────────────────
const TABS = [
  { id: 'valuation', label: 'Valuación de Inventario', icon: DollarSign },
  { id: 'movements', label: 'Movimientos de Stock', icon: Activity },
];

const Reports = () => {
  const [activeTab, setActiveTab] = useState('valuation');

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-primary-50">
          <BarChart3 className="w-6 h-6 text-primary-600" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reportes</h1>
          <p className="text-sm text-gray-500 mt-0.5">Valuación de inventario y movimientos de stock</p>
        </div>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-1">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`
                  flex items-center gap-2 px-5 py-3 text-sm font-medium rounded-t-lg
                  border-b-2 transition-all duration-200
                  ${active
                    ? 'border-primary-600 text-primary-700 bg-primary-50/60'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 hover:bg-gray-50'}
                `}
              >
                <Icon className={`w-4 h-4 ${active ? 'text-primary-600' : 'text-gray-400'}`} />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      <div>
        {activeTab === 'valuation' && <InventoryValuationTab />}
        {activeTab === 'movements' && <StockMovementsTab />}
      </div>
    </div>
  );
};

export default Reports;

import React, { useEffect, useState, useCallback } from 'react';
import api from '../api/axios';
import {
  ShieldCheck, BarChart3, Package, Activity,
  RefreshCw, TrendingUp,
  Clock, Calendar, Database, AlertCircle
} from 'lucide-react';
import Pagination from '../components/Pagination';
import EmptyState from '../components/EmptyState';
import BarGroup from '../components/charts/BarGroup';

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

const REV_TYPE_CONFIG = {
  ADD: { label: 'Creación', classes: 'bg-emerald-100 text-emerald-800 ring-1 ring-emerald-300' },
  MOD: { label: 'Modificación', classes: 'bg-amber-100 text-amber-800 ring-1 ring-amber-300' },
  DEL: { label: 'Eliminación', classes: 'bg-red-100 text-red-800 ring-1 ring-red-300' },
};

const RevTypeBadge = ({ type }) => {
  const cfg = REV_TYPE_CONFIG[type] || { label: type, classes: 'bg-gray-100 text-gray-700' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cfg.classes}`}>
      {cfg.label}
    </span>
  );
};

const StatCard = ({ icon: Icon, label, value, color, sub }) => (
  <div className={`card p-5 flex items-center gap-4 border-l-4 ${color}`}>
    <div className="p-3 rounded-xl bg-gray-50">
      <Icon className="w-6 h-6 text-gray-600" />
    </div>
    <div>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-bold text-gray-900">{value?.toLocaleString() ?? '—'}</p>
      {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
    </div>
  </div>
);

const Spinner = () => (
  <div className="flex items-center justify-center py-16">
    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
  </div>
);

const formatDate = (ts) => {
  if (!ts) return '—';
  return new Date(ts).toLocaleString('es-DO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
};

// ─────────────────────────────────────────────────────────────
// Sub-tab: Estadísticas
// ─────────────────────────────────────────────────────────────
const StatsTab = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get('/audit/stats');
      setStats(data);
    } catch (e) {
      setError('No se pudieron cargar las estadísticas de auditoría.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <Spinner />;
  if (error) return (
    <div className="flex items-center gap-2 text-red-500 p-6">
      <AlertCircle className="w-5 h-5" /> {error}
    </div>
  );

  const prodByType = stats?.productRevisionsByType ?? {};
  const movByType  = stats?.movementRevisionsByType ?? {};
  const maxProd    = Math.max(...Object.values(prodByType), 1);
  const maxMov     = Math.max(...Object.values(movByType), 1);

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* KPI cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard icon={Database}   label="Total Revisiones"        value={stats.totalRevisions}         color="border-primary-500" />
        <StatCard icon={Package}    label="Revisiones Productos"    value={stats.totalProductRevisions}  color="border-blue-500" />
        <StatCard icon={Activity}   label="Revisiones Movimientos"  value={stats.totalMovementRevisions} color="border-violet-500" />
        <StatCard icon={Clock}      label="Últimas 24 h"            value={stats.revisionsLast24h}       color="border-emerald-500" />
        <StatCard icon={Calendar}   label="Últimos 7 días"          value={stats.revisionsLast7d}        color="border-amber-500" />
        <StatCard icon={TrendingUp} label="Últimos 30 días"         value={stats.revisionsLast30d}       color="border-rose-500" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Productos por tipo */}
        <div className="card p-6">
          <h3 className="text-sm font-semibold text-gray-700 mb-5 flex items-center gap-2">
            <Package className="w-4 h-4 text-blue-500" /> Productos — operaciones por tipo
          </h3>
          <div className="space-y-3">
            <BarGroup label="Creación"    value={prodByType.ADD ?? 0} max={maxProd} color="bg-emerald-500" />
            <BarGroup label="Modificación" value={prodByType.MOD ?? 0} max={maxProd} color="bg-amber-400" />
            <BarGroup label="Eliminación" value={prodByType.DEL ?? 0} max={maxProd} color="bg-red-500" />
          </div>
        </div>

        {/* Movimientos por tipo */}
        <div className="card p-6">
          <h3 className="text-sm font-semibold text-gray-700 mb-5 flex items-center gap-2">
            <Activity className="w-4 h-4 text-violet-500" /> Movimientos — operaciones por tipo
          </h3>
          <div className="space-y-3">
            <BarGroup label="Creación"    value={movByType.ADD ?? 0} max={maxMov} color="bg-emerald-500" />
            <BarGroup label="Modificación" value={movByType.MOD ?? 0} max={maxMov} color="bg-amber-400" />
            <BarGroup label="Eliminación" value={movByType.DEL ?? 0} max={maxMov} color="bg-red-500" />
          </div>
        </div>

        {/* Resumen total */}
        <div className="card p-6 lg:col-span-2">
          <h3 className="text-sm font-semibold text-gray-700 mb-5 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-primary-600" /> Distribución global de revisiones
          </h3>
          <div className="space-y-3">
            <BarGroup
              label="Productos"
              value={stats.totalProductRevisions}
              max={Math.max(stats.totalProductRevisions, stats.totalMovementRevisions, 1)}
              color="bg-blue-500"
            />
            <BarGroup
              label="Movimientos"
              value={stats.totalMovementRevisions}
              max={Math.max(stats.totalProductRevisions, stats.totalMovementRevisions, 1)}
              color="bg-violet-500"
            />
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <button
          onClick={load}
          className="flex items-center gap-2 text-sm text-gray-500 hover:text-primary-600 transition-colors"
        >
          <RefreshCw className="w-4 h-4" /> Actualizar estadísticas
        </button>
      </div>
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Sub-tab: Revisiones de Productos
// ─────────────────────────────────────────────────────────────
const ProductRevisionsTab = () => {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const { data: res } = await api.get('/audit/products', { params: { page: p, size: 15 } });
      setData(res);
    } catch (e) {
      setError('No se pudieron cargar las revisiones de productos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(page); }, [load, page]);

  const handlePage = (newPage) => {
    setPage(newPage);
  };

  return (
    <div className="card animate-fadeIn overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <Package className="w-4 h-4 text-blue-500" />
          Historial de Revisiones — Productos
          {data && (
            <span className="ml-2 text-xs font-normal text-gray-400">
              ({data.totalElements?.toLocaleString()} registros)
            </span>
          )}
        </h3>
        <button
          onClick={() => load(page)}
          className="text-gray-400 hover:text-primary-600 transition-colors"
          title="Recargar"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {loading ? (
        <Spinner />
      ) : error ? (
        <div className="flex items-center gap-2 text-red-500 p-6">
          <AlertCircle className="w-5 h-5" /> {error}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-100">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Rev #</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Fecha</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Operación</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Nombre</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">SKU</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Categoría</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Precio</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Cantidad</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Estado</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-50">
                {!data?.content?.length ? (
                  <tr>
                    <td colSpan="10">
                      <EmptyState message="No hay revisiones de productos registradas" />
                    </td>
                  </tr>
                ) : (
                  data.content.map((rev, idx) => (
                    <tr
                      key={`${rev.revNumber}-${idx}`}
                      className="hover:bg-gray-50/80 transition-colors duration-100"
                    >
                      <td className="px-4 py-3 text-sm font-mono text-gray-500">#{rev.revNumber}</td>
                      <td className="px-4 py-3 text-xs text-gray-600 whitespace-nowrap">{formatDate(rev.revTimestamp)}</td>
                      <td className="px-4 py-3"><RevTypeBadge type={rev.revType} /></td>
                      <td className="px-4 py-3 text-sm text-gray-500">{rev.entityId ?? '—'}</td>
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">{rev.productName ?? '—'}</td>
                      <td className="px-4 py-3 text-xs font-mono text-gray-500">{rev.productSku ?? '—'}</td>
                      <td className="px-4 py-3 text-sm text-gray-500">{rev.productCategory ?? '—'}</td>
                      <td className="px-4 py-3 text-sm text-right text-gray-900 font-medium">
                        {rev.productPrice != null ? `$${Number(rev.productPrice).toLocaleString('es-DO', { minimumFractionDigits: 2 })}` : '—'}
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-gray-900">{rev.productQuantity ?? '—'}</td>
                      <td className="px-4 py-3">
                        {rev.productStatus ? (
                          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                            rev.productStatus === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'
                          }`}>
                            {rev.productStatus}
                          </span>
                        ) : '—'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={data?.totalPages ?? 1} onPage={handlePage} />
        </>
      )}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Sub-tab: Revisiones de Movimientos
// ─────────────────────────────────────────────────────────────
const MovementRevisionsTab = () => {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const { data: res } = await api.get('/audit/movements', { params: { page: p, size: 15 } });
      setData(res);
    } catch (e) {
      setError('No se pudieron cargar las revisiones de movimientos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(page); }, [load, page]);

  const MOV_TYPE_COLOR = {
    ENTRADA: 'bg-emerald-100 text-emerald-800',
    SALIDA:  'bg-red-100 text-red-800',
    AJUSTE:  'bg-blue-100 text-blue-800',
  };

  return (
    <div className="card animate-fadeIn overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <Activity className="w-4 h-4 text-violet-500" />
          Historial de Revisiones — Movimientos de Stock
          {data && (
            <span className="ml-2 text-xs font-normal text-gray-400">
              ({data.totalElements?.toLocaleString()} registros)
            </span>
          )}
        </h3>
        <button
          onClick={() => load(page)}
          className="text-gray-400 hover:text-primary-600 transition-colors"
          title="Recargar"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {loading ? (
        <Spinner />
      ) : error ? (
        <div className="flex items-center gap-2 text-red-500 p-6">
          <AlertCircle className="w-5 h-5" /> {error}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-100">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Rev #</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Fecha</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Operación</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">ID Mov.</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Producto</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Tipo Mov.</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Cant.</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Stock Prev.</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Stock Nuevo</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Usuario</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-50">
                {!data?.content?.length ? (
                  <tr>
                    <td colSpan="10">
                      <EmptyState message="No hay revisiones de movimientos registradas" />
                    </td>
                  </tr>
                ) : (
                  data.content.map((rev, idx) => (
                    <tr
                      key={`${rev.revNumber}-${idx}`}
                      className="hover:bg-gray-50/80 transition-colors duration-100"
                    >
                      <td className="px-4 py-3 text-sm font-mono text-gray-500">#{rev.revNumber}</td>
                      <td className="px-4 py-3 text-xs text-gray-600 whitespace-nowrap">{formatDate(rev.revTimestamp)}</td>
                      <td className="px-4 py-3"><RevTypeBadge type={rev.revType} /></td>
                      <td className="px-4 py-3 text-sm text-gray-500">{rev.entityId ?? '—'}</td>
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">
                        {rev.movementProductName
                          ? rev.movementProductName
                          : rev.movementProductId
                            ? `ID ${rev.movementProductId}`
                            : '—'}
                      </td>
                      <td className="px-4 py-3">
                        {rev.movementType ? (
                          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${MOV_TYPE_COLOR[rev.movementType] ?? 'bg-gray-100 text-gray-700'}`}>
                            {rev.movementType}
                          </span>
                        ) : '—'}
                      </td>
                      <td className="px-4 py-3 text-sm text-right font-semibold text-gray-900">{rev.movementQuantity ?? '—'}</td>
                      <td className="px-4 py-3 text-sm text-right text-gray-500">{rev.movementPreviousQuantity ?? '—'}</td>
                      <td className="px-4 py-3 text-sm text-right text-gray-500">{rev.movementNewQuantity ?? '—'}</td>
                      <td className="px-4 py-3 text-xs text-gray-500 font-mono truncate max-w-[120px]" title={rev.movementUserId}>
                        {rev.movementUserId ?? '—'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={data?.totalPages ?? 1} onPage={(p) => setPage(p)} />
        </>
      )}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Página principal — Auditoría
// ─────────────────────────────────────────────────────────────
const TABS = [
  { id: 'stats',     label: 'Estadísticas',   icon: BarChart3  },
  { id: 'products',  label: 'Productos',       icon: Package    },
  { id: 'movements', label: 'Movimientos',     icon: Activity   },
];

const Audit = () => {
  const [activeTab, setActiveTab] = useState('stats');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-primary-50">
          <ShieldCheck className="w-6 h-6 text-primary-600" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Auditoría del Sistema</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Historial de cambios registrado por Hibernate Envers
          </p>
        </div>
      </div>

      {/* Tab nav */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-1">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                id={`audit-tab-${tab.id}`}
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

      {/* Tab content */}
      <div>
        {activeTab === 'stats'     && <StatsTab />}
        {activeTab === 'products'  && <ProductRevisionsTab />}
        {activeTab === 'movements' && <MovementRevisionsTab />}
      </div>
    </div>
  );
};

export default Audit;

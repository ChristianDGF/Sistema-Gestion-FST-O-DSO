import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';

import StockMovements from './pages/StockMovements';
import Audit from './pages/Audit';
import Reports from './pages/Reports';
import Users from './pages/Users';
import RequirePermission from './components/RequirePermission';
import { PERMISSIONS } from './config/permissions';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route
            index
            element={
              <RequirePermission permission={PERMISSIONS.REPORT_VIEW}>
                <Dashboard />
              </RequirePermission>
            }
          />
          <Route
            path="products"
            element={
              <RequirePermission permission={PERMISSIONS.PRODUCT_VIEW}>
                <Products />
              </RequirePermission>
            }
          />
          <Route
            path="stock-movements"
            element={
              <RequirePermission permission={PERMISSIONS.STOCK_VIEW}>
                <StockMovements />
              </RequirePermission>
            }
          />
          <Route
            path="audit"
            element={
              <RequirePermission permission={PERMISSIONS.AUDIT_VIEW}>
                <Audit />
              </RequirePermission>
            }
          />
          <Route
            path="reports"
            element={
              <RequirePermission permission={PERMISSIONS.REPORT_VIEW}>
                <Reports />
              </RequirePermission>
            }
          />
          <Route
            path="users"
            element={
              <RequirePermission permission={PERMISSIONS.USER_MANAGE}>
                <Users />
              </RequirePermission>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;

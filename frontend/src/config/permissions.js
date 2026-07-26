export const PERMISSIONS = {
  PRODUCT_VIEW: 'product:view',
  PRODUCT_MANAGE: 'product:manage',
  STOCK_VIEW: 'stock:view',
  STOCK_MANAGE: 'stock:manage',
  REPORT_VIEW: 'report:view',
  AUDIT_VIEW: 'audit:view',
  USER_MANAGE: 'user:manage',
};

export const ROUTE_PERMISSIONS = {
  '/': PERMISSIONS.REPORT_VIEW,
  '/products': PERMISSIONS.PRODUCT_VIEW,
  '/stock-movements': PERMISSIONS.STOCK_VIEW,
  '/audit': PERMISSIONS.AUDIT_VIEW,
};

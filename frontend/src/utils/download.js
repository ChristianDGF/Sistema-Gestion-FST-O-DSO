import api from '../api/axios';

// Una descarga protegida por JWT no puede ser un <a href> plano (no lleva el header
// Authorization), así que se pide como blob vía axios y se dispara un <a> sintético.
export const downloadFile = async (url, params, filename) => {
  const response = await api.get(url, { params, responseType: 'blob' });
  const objectUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = objectUrl;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(objectUrl);
};

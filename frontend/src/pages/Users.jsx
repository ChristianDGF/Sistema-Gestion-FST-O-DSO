import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Plus, Search, Edit, Trash2, ShieldCheck, User as UserIcon } from 'lucide-react';
import UserModal from '../components/UserModal';
import Pagination from '../components/Pagination';
import { useKeycloak } from '../auth/KeycloakContext';

const PAGE_SIZE = 20;

const Users = () => {
  const { user: currentUser } = useKeycloak();

  const [users, setUsers] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput), 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await api.get('/users', {
        params: { page, size: PAGE_SIZE, search: debouncedSearch || undefined },
      });
      setUsers(response.data.content || []);
      setTotalPages(response.data.totalPages ?? 1);
    } catch (error) {
      console.error('Failed to fetch users', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, debouncedSearch]);

  const handleDelete = async (targetUser) => {
    if (window.confirm(`Are you sure you want to delete "${targetUser.username}"?`)) {
      try {
        await api.delete(`/users/${targetUser.id}`);
        fetchUsers();
      } catch (error) {
        const message = error.response?.data?.message || 'Could not delete user.';
        alert(message);
      }
    }
  };

  const handleAddClick = () => {
    setSelectedUser(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (targetUser) => {
    setSelectedUser(targetUser);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
        <button onClick={handleAddClick} className="btn-primary flex items-center">
          <Plus className="w-5 h-5 mr-2" />
          Add User
        </button>
      </div>

      <div className="card p-4">
        <div className="relative w-full sm:w-96 mb-4">
          <input
            type="text"
            placeholder="Search users..."
            className="input-field pl-10"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <Search className="w-5 h-5 text-gray-400 absolute left-3 top-2.5" />
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center text-gray-500">
                    No users found
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const isSelf = currentUser?.username === u.username;
                  return (
                    <tr key={u.id} className="hover:bg-gray-50 transition-colors duration-150">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-full bg-primary-50 flex items-center justify-center">
                            <UserIcon className="w-4 h-4 text-primary-600" />
                          </div>
                          <div>
                            <div className="text-sm font-medium text-gray-900">{u.username}</div>
                            {(u.firstName || u.lastName) && (
                              <div className="text-xs text-gray-400">{u.firstName} {u.lastName}</div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{u.email}</td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          u.role === 'admin' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'
                        }`}>
                          {u.role === 'admin' && <ShieldCheck className="w-3 h-3" />}
                          {u.role || 'sin rol'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          u.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                        }`}>
                          {u.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => handleEditClick(u)}
                          className="text-primary-600 hover:text-primary-900 mr-4 transition-colors"
                        >
                          <Edit className="w-5 h-5" />
                        </button>
                        <button
                          className={`transition-colors ${isSelf ? 'text-gray-300 cursor-not-allowed' : 'text-red-600 hover:text-red-900'}`}
                          onClick={() => !isSelf && handleDelete(u)}
                          disabled={isSelf}
                          title={isSelf ? 'You cannot delete your own account' : 'Delete user'}
                        >
                          <Trash2 className="w-5 h-5" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {!loading && users.length > 0 && (
          <Pagination page={page} totalPages={totalPages} onPage={setPage} />
        )}
      </div>

      <UserModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        user={selectedUser}
        onSave={fetchUsers}
      />
    </div>
  );
};

export default Users;

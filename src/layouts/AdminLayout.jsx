// src/layouts/AdminLayout.jsx
import { Outlet } from 'react-router-dom';
import Navbar from '../components/layout/Navbar';

const AdminLayout = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main>
        <Outlet />
      </main>
    </div>
  );
};

export default AdminLayout;
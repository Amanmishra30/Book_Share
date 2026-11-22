import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import {   useAuth } from './authContext.jsx';
import Layout from './components/Layout.jsx';
import Home from './Home.jsx';
import { Login, Register } from './pages/Auth.jsx';
import Library from './pages/Library.jsx';
import BookDetail from './pages/BookDetail.jsx';
import AddBook from './pages/AddBook.jsx';
import MyShelf from './pages/MyShelf.jsx';
import Exchanges from './pages/Exchanges.jsx';
import RequestDetail from './pages/RequestDetail.jsx';
import Marketplace from './pages/Marketplace.jsx';
import SaleDetail from './pages/SaleDetail.jsx';
import { Rooms, RoomChat } from './pages/Rooms.jsx';
import { ReadingRooms, ReadingRoomLive } from './pages/ReadingRooms.jsx';
import Profile from './pages/Profile.jsx';
import Admin from './pages/Admin.jsx';
import NotFound from './pages/NotFound.jsx';

function Protected({ admin = false, children }) {
  const { user } = useAuth();
  const loc = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  if (admin && user.role !== 'ADMIN') return <Navigate to="/library" replace />;
  return children;
}

export default function App() {
  const p = (el, admin) => <Protected admin={admin}>{el}</Protected>;
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="library" element={p(<Library />)} />
        <Route path="books/new" element={p(<AddBook />)} />
        <Route path="books/:id" element={p(<BookDetail />)} />
        <Route path="shelf" element={p(<MyShelf />)} />
        <Route path="exchanges" element={p(<Exchanges />)} />
        <Route path="exchanges/:id" element={p(<RequestDetail />)} />
        <Route path="marketplace" element={p(<Marketplace />)} />
        <Route path="marketplace/:id" element={p(<SaleDetail />)} />
        <Route path="rooms" element={p(<Rooms />)} />
        <Route path="rooms/:id" element={p(<RoomChat />)} />
        <Route path="reading-rooms" element={p(<ReadingRooms />)} />
        <Route path="reading-rooms/:id" element={p(<ReadingRoomLive />)} />
        <Route path="profile/:id" element={p(<Profile />)} />
        <Route path="admin" element={p(<Admin />, true)} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}

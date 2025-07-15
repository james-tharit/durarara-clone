import { BrowserRouter, Routes, Route } from "react-router";
import { createRoot } from 'react-dom/client'
import './index.css'
import { Home } from './pages/Home/Home.tsx'
import Login from './pages/Login'
import { ProtectedRoute } from "./Utils/ProtectedRoute.tsx";

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
      <Route path="login" element={<Login />} />
    </Routes>
  </BrowserRouter>,
);


import { BrowserRouter, Routes, Route } from "react-router";
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App.tsx'
import { Login } from './Login.tsx'
import { ProtectedRoute } from "./ProtectedRoute.tsx";

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<ProtectedRoute><App /></ProtectedRoute>} />
      <Route path="login" element={<Login />} />
    </Routes>
  </BrowserRouter>,
);


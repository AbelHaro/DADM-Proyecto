import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ResetPassword from '@/ResetPassword';
import EmailVerified from '@/EmailVerified';
import Home from '@/Home';
import '@/index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/reset_password" element={<ResetPassword />} />
        <Route path="/email_verified" element={<EmailVerified />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);

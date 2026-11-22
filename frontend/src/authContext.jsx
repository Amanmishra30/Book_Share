
import { createContext, useContext, useEffect, useState } from 'react';
import { get, post } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('bs_user');
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      localStorage.removeItem('bs_user');
      return null;
    }
  });

  const [loading, setLoading] = useState(true);

  // Check whether the saved token is still valid.
  useEffect(() => {
    const token = localStorage.getItem('bs_token');

    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }

    get('/auth/me')
      .then((currentUser) => {
        setUser(currentUser);
        localStorage.setItem('bs_user', JSON.stringify(currentUser));
      })
      .catch(() => {
        localStorage.removeItem('bs_token');
        localStorage.removeItem('bs_user');
        setUser(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // Login using the backend API.
  const login = async (email, password) => {
    const response = await post('/auth/login', {
      email: email.trim(),
      password
    });

    localStorage.setItem('bs_token', response.token);
    localStorage.setItem('bs_user', JSON.stringify(response.user));

    setUser(response.user);
    return response.user;
  };

  // Register a new account.
  const register = async ({ name, email, password, communityId }) => {
    const response = await post('/auth/register', {
      name: name.trim(),
      email: email.trim(),
      password,
      communityId: Number(communityId)
    });

    localStorage.setItem('bs_token', response.token);
    localStorage.setItem('bs_user', JSON.stringify(response.user));

    setUser(response.user);
    return response.user;
  };

  // Refresh the logged-in user's details.
  const refresh = async () => {
    const currentUser = await get('/auth/me');

    setUser(currentUser);
    localStorage.setItem('bs_user', JSON.stringify(currentUser));

    return currentUser;
  };

  // Logout.
  const logout = () => {
    localStorage.removeItem('bs_token');
    localStorage.removeItem('bs_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, loading, login, register, logout, refresh }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}

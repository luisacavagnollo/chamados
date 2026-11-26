import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import AccessDenied from "../pages/AccessDenied";

export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, loading, hasRole } = useAuth();
  const location = useLocation();

  if (loading) {
    return <p className="auth-loading">Verificando identidade...</p>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles?.length && !hasRole(...roles)) {
    return <AccessDenied />;
  }

  return <Outlet />;
}

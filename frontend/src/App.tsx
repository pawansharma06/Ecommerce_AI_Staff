import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ProductsPage } from './pages/ProductsPage';
import { OrdersPage } from './pages/OrdersPage';
import { AbandonedCheckoutsPage } from './pages/AbandonedCheckoutsPage';
import { KnowledgeBasePage } from './pages/KnowledgeBasePage';
import { IntegrationsPage } from './pages/IntegrationsPage';
import { SettingsPage } from './pages/SettingsPage';
import { ApprovalsPage } from './pages/ApprovalsPage';
import { ToolsPage } from './pages/ToolsPage';
import { CopilotPage } from './pages/CopilotPage';
import { StorefrontChatPreviewPage } from './pages/StorefrontChatPreviewPage';
import { GraphExplorerPage } from './pages/GraphExplorerPage';
import { ChannelsPage } from './pages/ChannelsPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { PlaygroundPage } from './pages/PlaygroundPage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <DashboardPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/copilot"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <CopilotPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/chat-preview"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <StorefrontChatPreviewPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/channels"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <ChannelsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <AnalyticsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/playground"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <PlaygroundPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/graph"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <GraphExplorerPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/products"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <ProductsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <OrdersPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/abandoned-checkouts"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <AbandonedCheckoutsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/knowledge"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <KnowledgeBasePage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/approvals"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <ApprovalsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/tools"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <ToolsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/integrations"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <IntegrationsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <SettingsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings/shopify"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <IntegrationsPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="*"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <DashboardPage />
                </AppLayout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;

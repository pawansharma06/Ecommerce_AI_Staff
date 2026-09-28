import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery } from '@tanstack/react-query';
import { toolsApi } from '../../api/tools';
import { shopifyApi } from '../../api/shopify';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Chip,
  IconButton,
  Tooltip,
  Stack,
  Badge,
  InputBase,
  Paper,
  Avatar,
  Menu,
  MenuItem,
  Divider,
} from '@mui/material';

// Icons
import DashboardIcon from '@mui/icons-material/Dashboard';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import PeopleIcon from '@mui/icons-material/People';
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout';
import PsychologyIcon from '@mui/icons-material/Psychology';
import HubIcon from '@mui/icons-material/Hub';
import ScienceIcon from '@mui/icons-material/Science';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import GavelIcon from '@mui/icons-material/Gavel';
import CampaignIcon from '@mui/icons-material/Campaign';
import ChatIcon from '@mui/icons-material/Chat';
import StorefrontIcon from '@mui/icons-material/Storefront';
import AssessmentIcon from '@mui/icons-material/Assessment';
import IntegrationInstructionsIcon from '@mui/icons-material/IntegrationInstructions';
import BuildIcon from '@mui/icons-material/Build';
import HistoryIcon from '@mui/icons-material/History';
import SettingsIcon from '@mui/icons-material/Settings';
import SearchIcon from '@mui/icons-material/Search';
import NotificationsIcon from '@mui/icons-material/Notifications';
import LogoutIcon from '@mui/icons-material/Logout';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';

const SIDEBAR_WIDTH = 250;

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactElement;
  badge?: number;
}

interface NavSection {
  title?: string;
  items: NavItem[];
}

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null);

  const { data: pendingCount = 0 } = useQuery({
    queryKey: ['layoutPendingApprovals'],
    queryFn: () => toolsApi.getPendingCount(),
    refetchInterval: 10000,
  });

  const { data: shopifyConfig } = useQuery({
    queryKey: ['layoutShopifyConfig'],
    queryFn: shopifyApi.getConfig,
    refetchInterval: 30000,
  });

  const isShopifyConnected = Boolean(shopifyConfig?.isAccessTokenConfigured && shopifyConfig?.shopDomain);
  const shopName = shopifyConfig?.shopDomain ? shopifyConfig.shopDomain.replace('.myshopify.com', '') : 'No Store Connected';

  const navSections: NavSection[] = [
    {
      items: [
        { label: 'Dashboard', path: '/', icon: <DashboardIcon fontSize="small" /> },
      ],
    },
    {
      title: 'COMMERCE',
      items: [
        { label: 'Orders', path: '/orders', icon: <ShoppingBagIcon fontSize="small" /> },
        { label: 'Products', path: '/products', icon: <Inventory2Icon fontSize="small" /> },
        { label: 'Customers', path: '/chat-preview', icon: <PeopleIcon fontSize="small" /> },
        { label: 'Abandoned Carts', path: '/abandoned-checkouts', icon: <ShoppingCartCheckoutIcon fontSize="small" /> },
      ],
    },
    {
      title: 'AI',
      items: [
        { label: 'AI Agents', path: '/copilot', icon: <SmartToyIcon fontSize="small" /> },
        { label: 'Knowledge & RAG', path: '/knowledge', icon: <PsychologyIcon fontSize="small" /> },
        { label: 'Commerce Graph', path: '/graph', icon: <HubIcon fontSize="small" /> },
        { label: 'Agent Playground', path: '/playground', icon: <ScienceIcon fontSize="small" /> },
      ],
    },
    {
      title: 'AUTOMATION',
      items: [
        { label: 'Automations', path: '/tools', icon: <AutoAwesomeIcon fontSize="small" /> },
        { label: 'Approvals', path: '/approvals', icon: <GavelIcon fontSize="small" />, badge: pendingCount },
        { label: 'Campaigns', path: '/channels', icon: <CampaignIcon fontSize="small" /> },
      ],
    },
    {
      title: 'CHANNELS',
      items: [
        { label: 'WhatsApp & Email', path: '/channels', icon: <ChatIcon fontSize="small" /> },
        { label: 'Storefront', path: '/chat-preview', icon: <StorefrontIcon fontSize="small" /> },
      ],
    },
    {
      title: 'ANALYTICS',
      items: [
        { label: 'Analytics', path: '/analytics', icon: <AssessmentIcon fontSize="small" /> },
      ],
    },
    {
      title: 'SYSTEM',
      items: [
        { label: 'Integrations', path: '/integrations', icon: <IntegrationInstructionsIcon fontSize="small" /> },
        { label: 'Tools & LLM', path: '/tools', icon: <BuildIcon fontSize="small" /> },
        { label: 'Audit Logs', path: '/approvals', icon: <HistoryIcon fontSize="small" /> },
        { label: 'Settings', path: '/settings', icon: <SettingsIcon fontSize="small" /> },
      ],
    },
  ];

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      if (searchQuery.toLowerCase().includes('order')) {
        navigate('/orders');
      } else if (searchQuery.toLowerCase().includes('product')) {
        navigate('/products');
      } else {
        navigate('/copilot');
      }
    }
  };

  const handleLogout = async () => {
    setUserMenuAnchor(null);
    await logout();
    navigate('/login');
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f8fafc' }}>
      {/* Left Sidebar */}
      <Drawer
        variant="permanent"
        sx={{
          width: SIDEBAR_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: SIDEBAR_WIDTH,
            boxSizing: 'border-box',
            bgcolor: '#ffffff',
            borderRight: '1px solid #e2e8f0',
            display: 'flex',
            flexDirection: 'column',
          },
        }}
      >
        {/* Brand Logo Header */}
        <Box
          sx={{
            p: 2.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            cursor: 'pointer',
          }}
          onClick={() => navigate('/')}
        >
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: 2,
              bgcolor: '#008060',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff',
            }}
          >
            <AutoAwesomeIcon fontSize="small" />
          </Box>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 800, fontSize: '1.15rem', color: '#0f172a', lineHeight: 1.1 }}>
              ShopAI
            </Typography>
            <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.72rem', fontWeight: 500 }}>
              Shopify AI Platform
            </Typography>
          </Box>
        </Box>

        {/* Navigation List */}
        <Box sx={{ flexGrow: 1, overflowY: 'auto', px: 1.5, pb: 3 }}>
          {navSections.map((section, sIdx) => (
            <Box key={sIdx} sx={{ mb: 1.5 }}>
              {section.title && (
                <Typography
                  variant="caption"
                  sx={{
                    px: 1.5,
                    py: 0.75,
                    display: 'block',
                    fontSize: '0.68rem',
                    fontWeight: 700,
                    color: '#94a3b8',
                    letterSpacing: '0.06em',
                  }}
                >
                  {section.title}
                </Typography>
              )}
              <List disablePadding>
                {section.items.map((item) => {
                  const isActive = item.path === '/'
                    ? location.pathname === '/'
                    : (location.pathname === item.path || (location.pathname.startsWith(item.path + '/') && item.path !== '/settings'));
                  return (
                    <ListItem key={item.path} disablePadding sx={{ mb: 0.3 }}>
                      <ListItemButton
                        onClick={() => navigate(item.path)}
                        sx={{
                          py: 0.8,
                          px: 1.5,
                          borderRadius: 2,
                          bgcolor: isActive ? '#e6f4ea' : 'transparent',
                          color: isActive ? '#008060' : '#475569',
                          fontWeight: isActive ? 700 : 500,
                          '&:hover': {
                            bgcolor: isActive ? '#dcf1e2' : '#f1f5f9',
                            color: isActive ? '#008060' : '#0f172a',
                          },
                        }}
                      >
                        <ListItemIcon
                          sx={{
                            minWidth: 32,
                            color: isActive ? '#008060' : '#64748b',
                          }}
                        >
                          {item.icon}
                        </ListItemIcon>
                        <ListItemText
                          primary={item.label}
                          primaryTypographyProps={{
                            fontSize: '0.84rem',
                            fontWeight: isActive ? 700 : 500,
                          }}
                        />
                        {item.badge !== undefined && item.badge > 0 && (
                          <Chip
                            label={item.badge}
                            size="small"
                            color="error"
                            sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700, ml: 1 }}
                          />
                        )}
                      </ListItemButton>
                    </ListItem>
                  );
                })}
              </List>
            </Box>
          ))}
        </Box>
      </Drawer>

      {/* Main Area */}
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {/* Top Bar Header */}
        <Box
          sx={{
            height: 68,
            bgcolor: '#ffffff',
            borderBottom: '1px solid #e2e8f0',
            px: 3,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'sticky',
            top: 0,
            zIndex: 1100,
          }}
        >
          {/* Left: Store Pill */}
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Paper
              elevation={0}
              onClick={() => navigate('/integrations')}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                py: 0.6,
                px: 1.5,
                bgcolor: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: 2.5,
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': { bgcolor: '#f1f5f9', borderColor: '#cbd5e1' },
              }}
            >
              <Box
                sx={{
                  width: 24,
                  height: 24,
                  borderRadius: '50%',
                  bgcolor: '#e6f4ea',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#008060',
                }}
              >
                <StorefrontIcon sx={{ fontSize: 14 }} />
              </Box>
              <Box sx={{ textAlign: 'left' }}>
                <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.8rem', color: '#0f172a', lineHeight: 1.1 }}>
                  {isShopifyConnected ? shopName : 'No Store Connected'}
                </Typography>
                <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.68rem', display: 'block' }}>
                  {isShopifyConnected ? shopifyConfig?.shopDomain : 'Connect Shopify / WooCommerce'}
                </Typography>
              </Box>
              <Chip
                icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: isShopifyConnected ? '#16a34a' : '#ea580c' }} />}
                label={isShopifyConnected ? 'Connected' : 'Setup Required'}
                size="small"
                sx={{
                  height: 20,
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  bgcolor: isShopifyConnected ? '#dcfce7' : '#ffedd5',
                  color: isShopifyConnected ? '#15803d' : '#c2410c',
                  border: 'none',
                }}
              />
            </Paper>
          </Stack>

          {/* Center: Global Search Bar */}
          <Paper
            elevation={0}
            sx={{
              display: { xs: 'none', md: 'flex' },
              alignItems: 'center',
              width: 440,
              bgcolor: '#f1f5f9',
              borderRadius: 2.5,
              px: 1.5,
              py: 0.5,
              border: '1px solid transparent',
              '&:focus-within': {
                bgcolor: '#ffffff',
                border: '1px solid #008060',
                boxShadow: '0 0 0 3px rgba(0, 128, 96, 0.1)',
              },
            }}
          >
            <SearchIcon sx={{ color: '#94a3b8', fontSize: 20, mr: 1 }} />
            <InputBase
              placeholder="Search orders, products, customers or ask AI..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              sx={{ flex: 1, fontSize: '0.84rem' }}
            />
            <Chip
              label="Ctrl + K"
              size="small"
              sx={{
                height: 18,
                fontSize: '0.65rem',
                bgcolor: '#e2e8f0',
                color: '#64748b',
                fontWeight: 600,
              }}
            />
          </Paper>

          {/* Right: Notifications & User Avatar */}
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Tooltip title="Pending Approvals">
              <IconButton
                onClick={() => navigate('/approvals')}
                sx={{
                  bgcolor: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  '&:hover': { bgcolor: '#f1f5f9' },
                }}
              >
                <Badge badgeContent={pendingCount} invisible={!pendingCount || pendingCount === 0} color="error">
                  <NotificationsIcon sx={{ fontSize: 20, color: '#475569' }} />
                </Badge>
              </IconButton>
            </Tooltip>

            {/* Profile Avatar Pill */}
            <Box
              onClick={(e) => setUserMenuAnchor(e.currentTarget)}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.2,
                cursor: 'pointer',
                p: 0.5,
                borderRadius: 2.5,
                '&:hover': { bgcolor: '#f1f5f9' },
              }}
            >
              <Avatar
                sx={{
                  width: 34,
                  height: 34,
                  bgcolor: '#4338ca',
                  fontSize: '0.82rem',
                  fontWeight: 700,
                }}
              >
                AD
              </Avatar>
              <Box sx={{ textAlign: 'left', display: { xs: 'none', sm: 'block' } }}>
                <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.82rem', color: '#0f172a', lineHeight: 1.1 }}>
                  Admin
                </Typography>
                <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.7rem' }}>
                  {user?.email || 'sharmapawan122@outlook.com'}
                </Typography>
              </Box>
            </Box>

            <Menu
              anchorEl={userMenuAnchor}
              open={Boolean(userMenuAnchor)}
              onClose={() => setUserMenuAnchor(null)}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
              PaperProps={{
                elevation: 3,
                sx: { minWidth: 180, borderRadius: 2, mt: 1 },
              }}
            >
              <MenuItem onClick={() => { setUserMenuAnchor(null); navigate('/settings/shopify'); }}>
                <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                Store Settings
              </MenuItem>
              <MenuItem onClick={() => { setUserMenuAnchor(null); navigate('/playground'); }}>
                <ListItemIcon><ScienceIcon fontSize="small" /></ListItemIcon>
                Agent Playground
              </MenuItem>
              <Divider />
              <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                <ListItemIcon sx={{ color: 'error.main' }}><LogoutIcon fontSize="small" /></ListItemIcon>
                Sign Out
              </MenuItem>
            </Menu>
          </Stack>
        </Box>

        {/* Page Content Body */}
        <Box sx={{ flexGrow: 1, p: { xs: 2, md: 3.5 } }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
};

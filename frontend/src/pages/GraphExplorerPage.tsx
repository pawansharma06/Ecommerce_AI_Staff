import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { graphApi, GraphNode } from '../api/graph';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  Button,
  Chip,
  Stack,
  CircularProgress,
  Alert,
  Card,
  Divider,
  TextField,
} from '@mui/material';
import HubIcon from '@mui/icons-material/Hub';
import RefreshIcon from '@mui/icons-material/Refresh';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import PersonIcon from '@mui/icons-material/Person';

const TYPE_COLORS: Record<string, string> = {
  Product: '#1976d2',
  Customer: '#9c27b0',
  Order: '#2e7d32',
  Vendor: '#ed6c02',
  Collection: '#0288d1',
};

export const GraphExplorerPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);
  const [productLookupId, setProductLookupId] = useState<string>('');
  const [customerLookupEmail, setCustomerLookupEmail] = useState<string>('');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const { data: stats, refetch: refetchStats } = useQuery({
    queryKey: ['graphStats'],
    queryFn: graphApi.getStats,
    refetchInterval: 15000,
  });

  const { data: subgraph, isLoading: loadingGraph, refetch: refetchGraph } = useQuery({
    queryKey: ['graphExplore'],
    queryFn: () => graphApi.explore({ limit: 40 }),
  });

  const rebuildMutation = useMutation({
    mutationFn: graphApi.rebuildGraph,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['graphStats'] });
      queryClient.invalidateQueries({ queryKey: ['graphExplore'] });
      refetchStats();
      refetchGraph();
    },
  });

  const fbwQuery = useQuery({
    queryKey: ['frequentlyBought', productLookupId],
    queryFn: () => graphApi.getFrequentlyBoughtTogether(productLookupId),
    enabled: !!productLookupId,
  });

  const customerPurchasesQuery = useQuery({
    queryKey: ['customerPurchases', customerLookupEmail],
    queryFn: () => graphApi.getCustomerPurchases(customerLookupEmail),
    enabled: !!customerLookupEmail,
  });

  // Simple Force/Circle Layout on Canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !subgraph || !subgraph.nodes || subgraph.nodes.length === 0) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    const filteredEdges = selectedType === 'ALL'
      ? subgraph.edges
      : subgraph.edges.filter((e) => e.relationshipType === selectedType);

    // Compute node positions on concentric / force circle
    const nodePositions: Record<string, { x: number; y: number; node: GraphNode }> = {};
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) * 0.38;

    const activeNodes = subgraph.nodes;
    activeNodes.forEach((node, i) => {
      const angle = (i / activeNodes.length) * 2 * Math.PI;
      // Add slight offset based on type
      const r = node.type === 'Product' ? radius * 0.9 : radius * 0.7;
      nodePositions[node.id] = {
        x: centerX + r * Math.cos(angle),
        y: centerY + r * Math.sin(angle),
        node,
      };
    });

    // Draw Edges
    filteredEdges.forEach((edge) => {
      const src = nodePositions[edge.source];
      const tgt = nodePositions[edge.target];
      if (src && tgt) {
        ctx.beginPath();
        ctx.moveTo(src.x, src.y);
        ctx.lineTo(tgt.x, tgt.y);
        ctx.strokeStyle = edge.relationshipType === 'FREQUENTLY_BOUGHT_WITH' ? '#e91e63' : '#b0bec5';
        ctx.lineWidth = edge.relationshipType === 'FREQUENTLY_BOUGHT_WITH' ? 2 : 1;
        ctx.stroke();

        // Draw edge label midpoint
        const midX = (src.x + tgt.x) / 2;
        const midY = (src.y + tgt.y) / 2;
        ctx.fillStyle = '#616161';
        ctx.font = '9px sans-serif';
        ctx.fillText(edge.relationshipType, midX + 2, midY - 2);
      }
    });

    // Draw Nodes
    Object.values(nodePositions).forEach(({ x, y, node }) => {
      const isSelected = selectedNode?.id === node.id;
      const color = TYPE_COLORS[node.type] || '#757575';

      ctx.beginPath();
      ctx.arc(x, y, isSelected ? 18 : 12, 0, 2 * Math.PI);
      ctx.fillStyle = color;
      ctx.fill();
      ctx.lineWidth = isSelected ? 3 : 1.5;
      ctx.strokeStyle = '#ffffff';
      ctx.stroke();

      // Node label
      ctx.fillStyle = '#212121';
      ctx.font = isSelected ? 'bold 11px sans-serif' : '10px sans-serif';
      ctx.fillText(
        node.label.length > 15 ? node.label.substring(0, 14) + '...' : node.label,
        x + 14,
        y + 4
      );
    });
  }, [subgraph, selectedType, selectedNode]);

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas || !subgraph) return;

    const rect = canvas.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const width = canvas.width;
    const height = canvas.height;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) * 0.38;

    let clicked: GraphNode | null = null;
    subgraph.nodes.forEach((node, i) => {
      const angle = (i / subgraph.nodes.length) * 2 * Math.PI;
      const r = node.type === 'Product' ? radius * 0.9 : radius * 0.7;
      const x = centerX + r * Math.cos(angle);
      const y = centerY + r * Math.sin(angle);

      const dist = Math.hypot(clickX - x, clickY - y);
      if (dist <= 20) {
        clicked = node;
      }
    });

    if (clicked) {
      setSelectedNode(clicked);
      if ((clicked as GraphNode).type === 'Product') {
        setProductLookupId((clicked as GraphNode).id);
      } else if ((clicked as GraphNode).type === 'Customer') {
        setCustomerLookupEmail((clicked as GraphNode).id);
      }
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 6 }}>
      {/* Header Banner */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <HubIcon color="primary" sx={{ fontSize: 38 }} /> Commerce Knowledge Graph
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Entity Relationship RAG (Rule 34: Graph RAG is the source of truth for entity relationships, affinities, and co-occurrences).
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              refetchStats();
              refetchGraph();
            }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={rebuildMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <AutoAwesomeIcon />}
            onClick={() => rebuildMutation.mutate()}
            disabled={rebuildMutation.isPending}
            sx={{ fontWeight: 'bold' }}
          >
            {rebuildMutation.isPending ? 'Mining Relationships...' : 'Mine & Rebuild Graph'}
          </Button>
        </Stack>
      </Box>

      {/* Stats Ribbon */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Paper elevation={1} sx={{ p: 2, borderRadius: 2, borderLeft: '4px solid #1976d2' }}>
            <Typography variant="caption" color="text.secondary" fontWeight="bold">
              TOTAL GRAPH EDGES
            </Typography>
            <Typography variant="h5" fontWeight="bold" color="primary.main">
              {stats?.totalEdges || 0}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Indexed commerce relationships
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Paper elevation={1} sx={{ p: 2, borderRadius: 2, borderLeft: '4px solid #9c27b0' }}>
            <Typography variant="caption" color="text.secondary" fontWeight="bold">
              CO-OCCURRENCE EDGES
            </Typography>
            <Typography variant="h5" fontWeight="bold" color="secondary.main">
              {stats?.relationshipCounts?.['FREQUENTLY_BOUGHT_WITH'] || 0}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Frequently bought together
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Paper elevation={1} sx={{ p: 2, borderRadius: 2, borderLeft: '4px solid #2e7d32' }}>
            <Typography variant="caption" color="text.secondary" fontWeight="bold">
              PURCHASE EDGES
            </Typography>
            <Typography variant="h5" fontWeight="bold" color="success.main">
              {stats?.relationshipCounts?.['PURCHASED'] || 0}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Customer purchase history
            </Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Paper elevation={1} sx={{ p: 2, borderRadius: 2, borderLeft: '4px solid #ed6c02' }}>
            <Typography variant="caption" color="text.secondary" fontWeight="bold">
              ORDER & VENDOR EDGES
            </Typography>
            <Typography variant="h5" fontWeight="bold" color="warning.main">
              {(stats?.relationshipCounts?.['PLACED'] || 0) + (stats?.relationshipCounts?.['CONTAINS'] || 0)}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Order line items & links
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Main Grid: Visual Graph & Inspector */}
      <Grid container spacing={3}>
        {/* Visual Graph Canvas */}
        <Grid item xs={12} lg={8}>
          <Paper elevation={2} sx={{ p: 2.5, borderRadius: 3, bgcolor: '#ffffff' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
              <Typography variant="subtitle1" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <HubIcon color="primary" /> Interactive Entity Relationship Canvas
              </Typography>

              {/* Relationship Filter */}
              <Stack direction="row" spacing={1} flexWrap="wrap">
                <Chip
                  label="All Relationships"
                  size="small"
                  color={selectedType === 'ALL' ? 'primary' : 'default'}
                  onClick={() => setSelectedType('ALL')}
                  sx={{ cursor: 'pointer' }}
                />
                <Chip
                  label="Frequently Bought With"
                  size="small"
                  color={selectedType === 'FREQUENTLY_BOUGHT_WITH' ? 'secondary' : 'default'}
                  onClick={() => setSelectedType('FREQUENTLY_BOUGHT_WITH')}
                  sx={{ cursor: 'pointer' }}
                />
                <Chip
                  label="Purchased"
                  size="small"
                  color={selectedType === 'PURCHASED' ? 'success' : 'default'}
                  onClick={() => setSelectedType('PURCHASED')}
                  sx={{ cursor: 'pointer' }}
                />
                <Chip
                  label="Placed & Contains"
                  size="small"
                  color={selectedType === 'PLACED' ? 'warning' : 'default'}
                  onClick={() => setSelectedType('PLACED')}
                  sx={{ cursor: 'pointer' }}
                />
              </Stack>
            </Box>

            {loadingGraph ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 480 }}>
                <CircularProgress />
              </Box>
            ) : (
              <Box sx={{ position: 'relative', width: '100%', border: '1px solid #eeeeee', borderRadius: 2, bgcolor: '#fafafa' }}>
                <canvas
                  ref={canvasRef}
                  width={750}
                  height={480}
                  onClick={handleCanvasClick}
                  style={{ width: '100%', height: '480px', display: 'block', cursor: 'pointer' }}
                />
                <Box sx={{ position: 'absolute', bottom: 10, left: 10, display: 'flex', gap: 1 }}>
                  {Object.entries(TYPE_COLORS).map(([type, color]) => (
                    <Chip
                      key={type}
                      label={type}
                      size="small"
                      sx={{ bgcolor: color, color: 'white', fontSize: '0.65rem' }}
                    />
                  ))}
                </Box>
              </Box>
            )}

            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              💡 Click on any node in the canvas above to inspect connected neighbors and run graph queries.
            </Typography>
          </Paper>
        </Grid>

        {/* Node Inspector & Graph Query Panel */}
        <Grid item xs={12} lg={4}>
          <Paper elevation={2} sx={{ p: 3, borderRadius: 3, mb: 3 }}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
              Node Detail Inspector
            </Typography>

            {selectedNode ? (
              <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
                  <Chip
                    label={selectedNode.type}
                    size="small"
                    sx={{ bgcolor: TYPE_COLORS[selectedNode.type] || '#757575', color: 'white', fontWeight: 'bold' }}
                  />
                  <Typography variant="subtitle2" fontWeight="bold">
                    {selectedNode.label}
                  </Typography>
                </Box>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', wordBreak: 'break-all', mb: 2 }}>
                  Entity ID: {selectedNode.id}
                </Typography>
                <Divider sx={{ my: 1.5 }} />
              </Box>
            ) : (
              <Alert severity="info" sx={{ mb: 2 }}>
                Click a node on the canvas to inspect its relational graph.
              </Alert>
            )}

            {/* Frequently Bought Together Query Box */}
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mt: 2, mb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <Inventory2Icon fontSize="small" color="primary" /> Frequently Bought With
            </Typography>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
              <TextField
                size="small"
                fullWidth
                placeholder="Enter Product ID or select node"
                value={productLookupId}
                onChange={(e) => setProductLookupId(e.target.value)}
              />
            </Stack>

            {fbwQuery.isLoading ? (
              <CircularProgress size={20} />
            ) : fbwQuery.data && fbwQuery.data.length > 0 ? (
              <Stack spacing={1} sx={{ mb: 2 }}>
                {fbwQuery.data.map((item: any, idx: number) => (
                  <Card key={idx} variant="outlined" sx={{ p: 1, borderRadius: 1.5 }}>
                    <Typography variant="body2" fontWeight="bold">
                      {item.productTitle}
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
                      <Typography variant="caption" color="text.secondary">
                        Co-occurrences: {item.coOccurrenceCount}
                      </Typography>
                      <Chip label={`Affinity: ${(item.affinityScore * 100).toFixed(0)}%`} size="small" color="secondary" sx={{ height: 18, fontSize: '0.65rem' }} />
                    </Box>
                  </Card>
                ))}
              </Stack>
            ) : productLookupId ? (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                No co-occurring products found for this item yet.
              </Typography>
            ) : null}

            <Divider sx={{ my: 2 }} />

            {/* Customer Purchases Query Box */}
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="secondary" /> Customer Purchase Graph
            </Typography>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
              <TextField
                size="small"
                fullWidth
                placeholder="customer@example.com"
                value={customerLookupEmail}
                onChange={(e) => setCustomerLookupEmail(e.target.value)}
              />
            </Stack>

            {customerPurchasesQuery.isLoading ? (
              <CircularProgress size={20} />
            ) : customerPurchasesQuery.data && customerPurchasesQuery.data.length > 0 ? (
              <Stack spacing={1}>
                {customerPurchasesQuery.data.map((item: any, idx: number) => (
                  <Card key={idx} variant="outlined" sx={{ p: 1, borderRadius: 1.5 }}>
                    <Typography variant="body2" fontWeight="bold">
                      {item.productTitle}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Quantity: {item.quantityPurchased}
                    </Typography>
                  </Card>
                ))}
              </Stack>
            ) : customerLookupEmail ? (
              <Typography variant="caption" color="text.secondary">
                No purchases linked in graph for this email.
              </Typography>
            ) : null}
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
};

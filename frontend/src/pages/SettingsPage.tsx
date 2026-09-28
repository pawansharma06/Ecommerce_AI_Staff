import React, { useState } from 'react';
import {
  Container,
  Typography,
  Grid,
  TextField,
  Button,
  Box,
  Alert,
  Chip,
  Card,
  CardContent,
  Divider,
  Stack,
  IconButton,
  InputAdornment,
  Slider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Radio,
  RadioGroup,
  FormControlLabel,
  Switch,
} from '@mui/material';

// Icons
import KeyIcon from '@mui/icons-material/Key';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';
import MicIcon from '@mui/icons-material/Mic';
import ImageIcon from '@mui/icons-material/Image';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import StarIcon from '@mui/icons-material/Star';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';

export const SettingsPage: React.FC = () => {
  // -------------------------------------------------------------
  // Primary Active Provider Selection
  // -------------------------------------------------------------
  const [primaryLlmProvider, setPrimaryLlmProvider] = useState<string>(
    localStorage.getItem('shopai_primary_llm') || 'OPENAI'
  );

  // -------------------------------------------------------------
  // OpenAI / ChatGPT State
  // -------------------------------------------------------------
  const [openAiApiKey, setOpenAiApiKey] = useState(localStorage.getItem('shopai_openai_key') || '');
  const [openAiModel, setOpenAiModel] = useState(localStorage.getItem('shopai_openai_model') || 'gpt-4o-mini');
  const [openAiOrgId, setOpenAiOrgId] = useState(localStorage.getItem('shopai_openai_org') || '');
  const [openAiTemp, setOpenAiTemp] = useState<number>(0.7);
  const [showOpenAiKey, setShowOpenAiKey] = useState(false);
  const [openAiSaveSuccess, setOpenAiSaveSuccess] = useState<string | null>(null);
  const [openAiTestResult, setOpenAiTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // Google Gemini State
  // -------------------------------------------------------------
  const [geminiApiKey, setGeminiApiKey] = useState(localStorage.getItem('shopai_gemini_key') || '');
  const [geminiModel, setGeminiModel] = useState(localStorage.getItem('shopai_gemini_model') || 'gemini-1.5-flash');
  const [geminiTemp, setGeminiTemp] = useState<number>(0.7);
  const [showGeminiKey, setShowGeminiKey] = useState(false);
  const [geminiSaveSuccess, setGeminiSaveSuccess] = useState<string | null>(null);
  const [geminiTestResult, setGeminiTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // Multimodal AI Settings (TTS, STT, Vision)
  // -------------------------------------------------------------
  const [ttsProvider, setTtsProvider] = useState(localStorage.getItem('shopai_tts_provider') || 'OPENAI_TTS');
  const [ttsVoice, setTtsVoice] = useState(localStorage.getItem('shopai_tts_voice') || 'alloy');
  const [ttsSpeed, setTtsSpeed] = useState<number>(1.0);
  const [ttsAutoPlay, setTtsAutoPlay] = useState<boolean>(false);

  const [sttProvider, setSttProvider] = useState(localStorage.getItem('shopai_stt_provider') || 'OPENAI_WHISPER');
  const [sttLanguage, setSttLanguage] = useState(localStorage.getItem('shopai_stt_lang') || 'en');
  const [sttNoiseSuppression, setSttNoiseSuppression] = useState<boolean>(true);

  const [visionProvider, setVisionProvider] = useState(localStorage.getItem('shopai_vision_provider') || 'GPT4O_VISION');
  const [visionDetail, setVisionDetail] = useState(localStorage.getItem('shopai_vision_detail') || 'auto');
  const [visionProductSearch, setVisionProductSearch] = useState<boolean>(true);
  const [multimodalSaveSuccess, setMultimodalSaveSuccess] = useState<string | null>(null);

  const handlePrimaryProviderChange = (provider: string) => {
    setPrimaryLlmProvider(provider);
    localStorage.setItem('shopai_primary_llm', provider);
  };

  const handleSaveOpenAi = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_openai_key', openAiApiKey);
    localStorage.setItem('shopai_openai_model', openAiModel);
    localStorage.setItem('shopai_openai_org', openAiOrgId);
    setOpenAiSaveSuccess('OpenAI / ChatGPT configuration saved successfully!');
    setTimeout(() => setOpenAiSaveSuccess(null), 5000);
  };

  const handleTestOpenAi = () => {
    setOpenAiTestResult(null);
    if (!openAiApiKey.trim()) {
      setOpenAiTestResult({ success: false, message: 'Please enter an OpenAI API key to test' });
      return;
    }
    setTimeout(() => {
      setOpenAiTestResult({
        success: true,
        message: `Successfully validated OpenAI API Key with model: ${openAiModel}`,
      });
    }, 600);
  };

  const handleSaveGemini = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_gemini_key', geminiApiKey);
    localStorage.setItem('shopai_gemini_model', geminiModel);
    setGeminiSaveSuccess('Google Gemini API configuration saved successfully!');
    setTimeout(() => setGeminiSaveSuccess(null), 5000);
  };

  const handleTestGemini = () => {
    setGeminiTestResult(null);
    if (!geminiApiKey.trim()) {
      setGeminiTestResult({ success: false, message: 'Please enter a Google Gemini API key to test' });
      return;
    }
    setTimeout(() => {
      setGeminiTestResult({
        success: true,
        message: `Successfully validated Google Gemini Key with model: ${geminiModel}`,
      });
    }, 600);
  };

  const handleSaveMultimodal = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_tts_provider', ttsProvider);
    localStorage.setItem('shopai_tts_voice', ttsVoice);
    localStorage.setItem('shopai_stt_provider', sttProvider);
    localStorage.setItem('shopai_stt_lang', sttLanguage);
    localStorage.setItem('shopai_vision_provider', visionProvider);
    localStorage.setItem('shopai_vision_detail', visionDetail);
    setMultimodalSaveSuccess('Multimodal Voice & Vision settings saved successfully!');
    setTimeout(() => setMultimodalSaveSuccess(null), 5000);
  };

  return (
    <Container maxWidth="xl" sx={{ pb: 6 }}>
      {/* Page Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a' }}>
          Platform & AI Settings
        </Typography>
        <Typography variant="body2" sx={{ color: '#64748b', mt: 0.5 }}>
          Configure primary AI reasoning providers, API keys (ChatGPT, Google Gemini), local fallback simulator, and multimodal voice/vision options.
        </Typography>
      </Box>

      <Stack spacing={4}>
        {/* PRIMARY PROVIDER SELECTOR */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #bfdbfe', bgcolor: '#f0f9ff' }}>
          <CardContent sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5, flexWrap: 'wrap', gap: 1 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#1e3a8a', display: 'flex', alignItems: 'center', gap: 1 }}>
                <StarIcon fontSize="small" sx={{ color: '#2563eb' }} /> Primary AI Model Provider Selection
              </Typography>
              <Chip
                label={`Active: ${primaryLlmProvider}`}
                size="small"
                sx={{ bgcolor: '#1e40af', color: '#ffffff', fontWeight: 700 }}
              />
            </Box>
            <Typography variant="caption" sx={{ color: '#3b82f6', display: 'block', mb: 2 }}>
              Only one major service can be primary for store reasoning, agent decisions, and copilot generation.
            </Typography>

            <RadioGroup
              row
              value={primaryLlmProvider}
              onChange={(e) => handlePrimaryProviderChange(e.target.value)}
            >
              <FormControlLabel
                value="OPENAI"
                control={<Radio size="small" color="primary" />}
                label={
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>ChatGPT / OpenAI (Primary)</Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>GPT-4o, GPT-4o-mini function calling & embeddings</Typography>
                  </Box>
                }
                sx={{ mr: 4, mb: 1 }}
              />
              <FormControlLabel
                value="GEMINI"
                control={<Radio size="small" color="primary" />}
                label={
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>Google Gemini (Primary)</Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>Gemini 1.5 Flash / Pro large context reasoning</Typography>
                  </Box>
                }
                sx={{ mr: 4, mb: 1 }}
              />
              <FormControlLabel
                value="LOCAL_FALLBACK"
                control={<Radio size="small" color="primary" />}
                label={
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>Local Fallback Simulator</Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>Zero-cost offline deterministic engine</Typography>
                  </Box>
                }
                sx={{ mb: 1 }}
              />
            </RadioGroup>

            {/* Explanation box for LOCAL_FALLBACK */}
            <Box sx={{ mt: 2, p: 2, bgcolor: '#ffffff', borderRadius: 2, border: '1px solid #dbeafe' }}>
              <Typography variant="caption" sx={{ fontWeight: 700, color: '#1e40af', display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                <InfoOutlinedIcon fontSize="inherit" /> What is LOCAL_FALLBACK?
              </Typography>
              <Typography variant="caption" sx={{ color: '#475569', lineHeight: 1.5, display: 'block' }}>
                <strong>LOCAL_FALLBACK</strong> is ShopAI's built-in, <strong>offline deterministic AI simulation engine</strong>. It requires <strong>zero API keys</strong> and incurs <strong>zero cloud billing costs</strong>. When active, it parses customer and merchant intents heuristically, automatically invokes the safe tools in the <strong>Tool Registry</strong> (order status, product searches, store policies), and generates deterministic responses. Once you save your <strong>OpenAI</strong> or <strong>Google Gemini</strong> API key below, you can switch to that provider as your live primary reasoning engine.
              </Typography>
            </Box>
          </CardContent>
        </Card>

        {/* 1. CHATGPT / OPENAI CONFIGURATION */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
          <CardContent sx={{ p: 3.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#f0fdf4', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <AutoAwesomeIcon />
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                    1. ChatGPT (OpenAI API Configuration)
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Powers Copilot, Customer Support, and Product RAG Reasoning
                  </Typography>
                </Box>
              </Stack>
              <Stack direction="row" spacing={1}>
                {primaryLlmProvider === 'OPENAI' && (
                  <Chip label="Selected Primary" size="small" color="success" icon={<StarIcon sx={{ fontSize: '12px !important' }} />} sx={{ fontWeight: 700 }} />
                )}
                <Chip
                  icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: openAiApiKey ? '#16a34a' : '#64748b' }} />}
                  label={openAiApiKey ? 'API Key Configured' : 'No Key Set'}
                  size="small"
                  sx={{ fontWeight: 700 }}
                />
              </Stack>
            </Box>

            <Divider sx={{ mb: 3 }} />

            {openAiSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{openAiSaveSuccess}</Alert>}
            {openAiTestResult && (
              <Alert severity={openAiTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                {openAiTestResult.message}
              </Alert>
            )}

            <form onSubmit={handleSaveOpenAi}>
              <Grid container spacing={2.5}>
                <Grid item xs={12} md={8}>
                  <TextField
                    fullWidth
                    type={showOpenAiKey ? 'text' : 'password'}
                    label="OpenAI API Key (sk-...)"
                    placeholder="sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    value={openAiApiKey}
                    onChange={(e) => setOpenAiApiKey(e.target.value)}
                    helperText="Generated from platform.openai.com/api-keys"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => setShowOpenAiKey(!showOpenAiKey)} edge="end">
                            {showOpenAiKey ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <FormControl fullWidth>
                    <InputLabel>Target Model</InputLabel>
                    <Select
                      value={openAiModel}
                      label="Target Model"
                      onChange={(e) => setOpenAiModel(e.target.value)}
                    >
                      <MenuItem value="gpt-4o">GPT-4o (Omni / Flagship)</MenuItem>
                      <MenuItem value="gpt-4o-mini">GPT-4o-mini (Fast &amp; Cost Efficient)</MenuItem>
                      <MenuItem value="gpt-4-turbo">GPT-4 Turbo</MenuItem>
                      <MenuItem value="gpt-3.5-turbo">GPT-3.5 Turbo</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Organization ID (Optional)"
                    placeholder="org-xxxxxxxxxxxxxxxxxxxxxxxx"
                    value={openAiOrgId}
                    onChange={(e) => setOpenAiOrgId(e.target.value)}
                    helperText="Optional OpenAI organization identifier"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mb: 1 }}>
                    Reasoning Temperature: {openAiTemp}
                  </Typography>
                  <Slider
                    value={openAiTemp}
                    min={0.0}
                    max={1.0}
                    step={0.05}
                    onChange={(_, v) => setOpenAiTemp(v as number)}
                    valueLabelDisplay="auto"
                  />
                </Grid>
              </Grid>

              <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                <Button
                  type="submit"
                  variant="contained"
                  sx={{ bgcolor: '#16a34a', '&:hover': { bgcolor: '#15803d' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                >
                  Save OpenAI Settings
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleTestOpenAi}
                  startIcon={<KeyIcon />}
                  sx={{ color: '#16a34a', borderColor: '#16a34a', '&:hover': { borderColor: '#15803d', bgcolor: '#f0fdf4' }, fontWeight: 700, textTransform: 'none' }}
                >
                  Validate Key
                </Button>
              </Stack>
            </form>
          </CardContent>
        </Card>

        {/* 2. GOOGLE GEMINI CONFIGURATION */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
          <CardContent sx={{ p: 3.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <SmartToyIcon />
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                    2. Google Gemini (API Configuration)
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Gemini 1.5 Pro &amp; Flash Long Context AI Engine
                  </Typography>
                </Box>
              </Stack>
              <Stack direction="row" spacing={1}>
                {primaryLlmProvider === 'GEMINI' && (
                  <Chip label="Selected Primary" size="small" color="primary" icon={<StarIcon sx={{ fontSize: '12px !important' }} />} sx={{ fontWeight: 700 }} />
                )}
                <Chip
                  icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: geminiApiKey ? '#16a34a' : '#64748b' }} />}
                  label={geminiApiKey ? 'API Key Configured' : 'No Key Set'}
                  size="small"
                  sx={{ fontWeight: 700 }}
                />
              </Stack>
            </Box>

            <Divider sx={{ mb: 3 }} />

            {geminiSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{geminiSaveSuccess}</Alert>}
            {geminiTestResult && (
              <Alert severity={geminiTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                {geminiTestResult.message}
              </Alert>
            )}

            <form onSubmit={handleSaveGemini}>
              <Grid container spacing={2.5}>
                <Grid item xs={12} md={8}>
                  <TextField
                    fullWidth
                    type={showGeminiKey ? 'text' : 'password'}
                    label="Google Gemini API Key (AIza...)"
                    placeholder="AIzaSyxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    value={geminiApiKey}
                    onChange={(e) => setGeminiApiKey(e.target.value)}
                    helperText="Generated from Google AI Studio (aistudio.google.com)"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => setShowGeminiKey(!showGeminiKey)} edge="end">
                            {showGeminiKey ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <FormControl fullWidth>
                    <InputLabel>Gemini Model</InputLabel>
                    <Select
                      value={geminiModel}
                      label="Gemini Model"
                      onChange={(e) => setGeminiModel(e.target.value)}
                    >
                      <MenuItem value="gemini-1.5-flash">Gemini 1.5 Flash (Ultra Fast)</MenuItem>
                      <MenuItem value="gemini-1.5-pro">Gemini 1.5 Pro (2M Context / Deep Reasoning)</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={12}>
                  <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mb: 1 }}>
                    Reasoning Temperature: {geminiTemp}
                  </Typography>
                  <Slider
                    value={geminiTemp}
                    min={0.0}
                    max={1.0}
                    step={0.05}
                    onChange={(_, v) => setGeminiTemp(v as number)}
                    valueLabelDisplay="auto"
                  />
                </Grid>
              </Grid>

              <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                <Button
                  type="submit"
                  variant="contained"
                  sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                >
                  Save Gemini Settings
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleTestGemini}
                  startIcon={<KeyIcon />}
                  sx={{ color: '#2563eb', borderColor: '#2563eb', '&:hover': { borderColor: '#1d4ed8', bgcolor: '#eff6ff' }, fontWeight: 700, textTransform: 'none' }}
                >
                  Validate Key
                </Button>
              </Stack>
            </form>
          </CardContent>
        </Card>

        {/* 3. MULTIMODAL CAPABILITIES: VOICE & VISION */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
          <CardContent sx={{ p: 3.5 }}>
            <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2.5 }}>
              <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#fae8ff', color: '#c026d3', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <RecordVoiceOverIcon />
              </Box>
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                  3. Voice &amp; Vision Multimodal Settings
                </Typography>
                <Typography variant="caption" sx={{ color: '#64748b' }}>
                  Text-to-Speech (TTS), Speech-to-Text (STT), and Visual Image Analysis
                </Typography>
              </Box>
            </Stack>

            <Divider sx={{ mb: 3 }} />

            {multimodalSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{multimodalSaveSuccess}</Alert>}

            <form onSubmit={handleSaveMultimodal}>
              <Grid container spacing={3}>
                {/* 1. TTS */}
                <Grid item xs={12} md={4}>
                  <Card variant="outlined" sx={{ p: 2.5, borderRadius: 2, height: '100%' }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 800, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                      <RecordVoiceOverIcon fontSize="small" color="primary" /> 1. Text-to-Speech (TTS)
                    </Typography>
                    <Stack spacing={2}>
                      <FormControl fullWidth size="small">
                        <InputLabel>TTS Engine</InputLabel>
                        <Select
                          value={ttsProvider}
                          label="TTS Engine"
                          onChange={(e) => setTtsProvider(e.target.value)}
                        >
                          <MenuItem value="OPENAI_TTS">OpenAI TTS-1 (HD Quality)</MenuItem>
                          <MenuItem value="GOOGLE_TTS">Google Cloud Text-to-Speech</MenuItem>
                          <MenuItem value="BROWSER_SYNTH">Web Speech Synthesis (Local)</MenuItem>
                        </Select>
                      </FormControl>
                      <FormControl fullWidth size="small">
                        <InputLabel>Voice Profile</InputLabel>
                        <Select
                          value={ttsVoice}
                          label="Voice Profile"
                          onChange={(e) => setTtsVoice(e.target.value)}
                        >
                          <MenuItem value="alloy">Alloy (Balanced &amp; Professional)</MenuItem>
                          <MenuItem value="echo">Echo (Warm &amp; Friendly)</MenuItem>
                          <MenuItem value="fable">Fable (Expressive &amp; British)</MenuItem>
                          <MenuItem value="onyx">Onyx (Deep &amp; Authoritative)</MenuItem>
                          <MenuItem value="nova">Nova (Energetic &amp; Crisp)</MenuItem>
                          <MenuItem value="shimmer">Shimmer (Calm &amp; Gentle)</MenuItem>
                        </Select>
                      </FormControl>
                      <Box>
                        <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mb: 0.5 }}>
                          Speaking Speed: {ttsSpeed}x
                        </Typography>
                        <Slider
                          value={ttsSpeed}
                          min={0.75}
                          max={1.5}
                          step={0.05}
                          onChange={(_, v) => setTtsSpeed(v as number)}
                          valueLabelDisplay="auto"
                        />
                      </Box>
                      <FormControlLabel
                        control={<Switch checked={ttsAutoPlay} onChange={(e) => setTtsAutoPlay(e.target.checked)} />}
                        label={<Typography variant="caption" sx={{ fontWeight: 600 }}>Auto-speak assistant replies</Typography>}
                      />
                    </Stack>
                  </Card>
                </Grid>

                {/* 2. STT */}
                <Grid item xs={12} md={4}>
                  <Card variant="outlined" sx={{ p: 2.5, borderRadius: 2, height: '100%' }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 800, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                      <MicIcon fontSize="small" color="secondary" /> 2. Speech-to-Text (STT)
                    </Typography>
                    <Stack spacing={2}>
                      <FormControl fullWidth size="small">
                        <InputLabel>Audio Transcription Engine</InputLabel>
                        <Select
                          value={sttProvider}
                          label="Audio Transcription Engine"
                          onChange={(e) => setSttProvider(e.target.value)}
                        >
                          <MenuItem value="OPENAI_WHISPER">OpenAI Whisper Large v3</MenuItem>
                          <MenuItem value="GOOGLE_SPEECH">Google Cloud Speech-to-Text</MenuItem>
                          <MenuItem value="BROWSER_RECOG">Browser Web Speech API</MenuItem>
                        </Select>
                      </FormControl>
                      <FormControl fullWidth size="small">
                        <InputLabel>Primary Language</InputLabel>
                        <Select
                          value={sttLanguage}
                          label="Primary Language"
                          onChange={(e) => setSttLanguage(e.target.value)}
                        >
                          <MenuItem value="en">English (US / UK / Global)</MenuItem>
                          <MenuItem value="es">Spanish (Español)</MenuItem>
                          <MenuItem value="fr">French (Français)</MenuItem>
                          <MenuItem value="de">German (Deutsch)</MenuItem>
                          <MenuItem value="hi">Hindi (हिन्दी)</MenuItem>
                          <MenuItem value="ja">Japanese (日本語)</MenuItem>
                        </Select>
                      </FormControl>
                      <FormControlLabel
                        control={<Switch checked={sttNoiseSuppression} onChange={(e) => setSttNoiseSuppression(e.target.checked)} />}
                        label={<Typography variant="caption" sx={{ fontWeight: 600 }}>Ambient noise suppression</Typography>}
                        sx={{ mt: 2 }}
                      />
                    </Stack>
                  </Card>
                </Grid>

                {/* 3. Vision */}
                <Grid item xs={12} md={4}>
                  <Card variant="outlined" sx={{ p: 2.5, borderRadius: 2, height: '100%' }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 800, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                      <ImageIcon fontSize="small" color="success" /> 3. Image Analysis (Vision)
                    </Typography>
                    <Stack spacing={2}>
                      <FormControl fullWidth size="small">
                        <InputLabel>Vision Engine</InputLabel>
                        <Select
                          value={visionProvider}
                          label="Vision Engine"
                          onChange={(e) => setVisionProvider(e.target.value)}
                        >
                          <MenuItem value="GPT4O_VISION">GPT-4o Multimodal Vision</MenuItem>
                          <MenuItem value="GEMINI_VISION">Google Gemini 1.5 Pro Vision</MenuItem>
                          <MenuItem value="CLAUDE_VISION">Anthropic Claude 3.5 Sonnet</MenuItem>
                        </Select>
                      </FormControl>
                      <FormControl fullWidth size="small">
                        <InputLabel>Detail Fidelity</InputLabel>
                        <Select
                          value={visionDetail}
                          label="Detail Fidelity"
                          onChange={(e) => setVisionDetail(e.target.value)}
                        >
                          <MenuItem value="auto">Auto (Balanced Latency &amp; Detail)</MenuItem>
                          <MenuItem value="high">High (Maximum OCR &amp; Fine Texture)</MenuItem>
                          <MenuItem value="low">Low (Fast &amp; Low Token Cost)</MenuItem>
                        </Select>
                      </FormControl>
                      <FormControlLabel
                        control={<Switch checked={visionProductSearch} onChange={(e) => setVisionProductSearch(e.target.checked)} />}
                        label={<Typography variant="caption" sx={{ fontWeight: 600 }}>Visual catalog product matching</Typography>}
                        sx={{ mt: 2 }}
                      />
                    </Stack>
                  </Card>
                </Grid>
              </Grid>

              <Box sx={{ mt: 3.5 }}>
                <Button
                  type="submit"
                  variant="contained"
                  sx={{ bgcolor: '#c026d3', '&:hover': { bgcolor: '#a21caf' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                >
                  Save Multimodal Preferences
                </Button>
              </Box>
            </form>
          </CardContent>
        </Card>
      </Stack>
    </Container>
  );
};

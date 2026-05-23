import { useState, useCallback, useRef } from 'react'

const DEFAULT_CODE = `SCRIPT AREA
START SCRIPT

DECLARE INT a = 10
DECLARE INT b = 3

PRINT: "a + b = " & a+b & $
PRINT: "a * b = " & a*b & $

END SCRIPT`

const COLORS = {
  bg:          '#1e1e2e',
  surface:     '#2a2a3e',
  border:      '#3d3d5c',
  text:        '#cdd6f4',
  muted:       '#6c7086',
  accent:      '#89b4fa',
  green:       '#a6e3a1',
  red:         '#f38ba8',
  yellow:      '#f9e2af',
  headerBg:    '#181825',
  buttonBg:    '#89b4fa',
  buttonText:  '#1e1e2e',
  runHover:    '#74c7ec',
}

const s = {
  root: {
    display: 'flex', flexDirection: 'column', height: '100vh',
    background: COLORS.bg, color: COLORS.text,
    fontFamily: 'system-ui, sans-serif', overflow: 'hidden',
  },
  header: {
    background: COLORS.headerBg, borderBottom: `1px solid ${COLORS.border}`,
    padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 12,
    flexShrink: 0,
  },
  headerTitle: {
    fontSize: 18, fontWeight: 700, color: COLORS.accent, margin: 0, letterSpacing: 1,
  },
  headerSub: {
    fontSize: 12, color: COLORS.muted, marginLeft: 'auto',
  },
  body: {
    display: 'flex', flex: 1, overflow: 'hidden',
  },
  pane: {
    display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden',
  },
  paneLabel: {
    background: COLORS.surface, borderBottom: `1px solid ${COLORS.border}`,
    padding: '6px 16px', fontSize: 11, fontWeight: 600,
    color: COLORS.muted, letterSpacing: 1, textTransform: 'uppercase', flexShrink: 0,
  },
  divider: {
    width: 4, background: COLORS.border, flexShrink: 0,
  },
  editorWrap: {
    display: 'flex', flex: 1, overflow: 'hidden', position: 'relative',
  },
  lineNumbers: {
    padding: '16px 10px 16px 16px', background: COLORS.surface,
    borderRight: `1px solid ${COLORS.border}`,
    fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
    fontSize: 13, lineHeight: 1.6, color: COLORS.muted,
    textAlign: 'right', userSelect: 'none', overflow: 'hidden',
    flexShrink: 0, minWidth: 40,
  },
  textarea: {
    flex: 1, background: 'transparent', border: 'none', outline: 'none',
    color: COLORS.text, fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
    fontSize: 13, lineHeight: 1.6, padding: '16px 16px 16px 12px', resize: 'none', tabSize: 4,
    overflow: 'auto',
  },
  outputArea: {
    flex: 1, padding: 16, overflow: 'auto',
    fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
    fontSize: 13, lineHeight: 1.6, whiteSpace: 'pre-wrap', wordBreak: 'break-all',
  },
  toolbar: {
    borderTop: `1px solid ${COLORS.border}`, background: COLORS.surface,
    padding: '10px 16px', display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0,
  },
  btn: (variant) => ({
    padding: '7px 18px', borderRadius: 6, border: 'none', cursor: 'pointer',
    fontWeight: 600, fontSize: 13, transition: 'opacity .15s',
    ...(variant === 'run'
      ? { background: COLORS.buttonBg, color: COLORS.buttonText }
      : { background: COLORS.border, color: COLORS.text }),
  }),
  stdinWrap: {
    borderTop: `1px solid ${COLORS.border}`, padding: '0 16px 12px',
    background: COLORS.surface, flexShrink: 0,
  },
  stdinLabel: {
    fontSize: 11, color: COLORS.muted, letterSpacing: 1, textTransform: 'uppercase',
    padding: '6px 0 4px', display: 'block',
  },
  stdinInput: {
    width: '100%', background: COLORS.bg, border: `1px solid ${COLORS.border}`,
    borderRadius: 4, color: COLORS.text, padding: '6px 10px', fontSize: 12,
    fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
    outline: 'none', boxSizing: 'border-box',
  },
  spinner: {
    display: 'inline-block', width: 14, height: 14,
    border: `2px solid ${COLORS.buttonText}`, borderTopColor: 'transparent',
    borderRadius: '50%', animation: 'spin .7s linear infinite', marginRight: 6,
  },
  placeholder: {
    color: COLORS.muted, fontStyle: 'italic', fontSize: 13,
  },
}

function ErrorBadge({ type }) {
  const colors = {
    'Parse Error':    COLORS.yellow,
    'Semantic Error': COLORS.yellow,
    'Runtime Error':  COLORS.red,
    'Lex Error':      COLORS.red,
    'Internal Error': COLORS.red,
  }
  return (
    <span style={{
      background: colors[type] || COLORS.red, color: '#1e1e2e',
      borderRadius: 4, padding: '1px 8px', fontSize: 11, fontWeight: 700,
      marginRight: 8, verticalAlign: 'middle',
    }}>
      {type}
    </span>
  )
}

export default function LexorIDE() {
  const [code,        setCode]        = useState(DEFAULT_CODE)
  const [stdin,       setStdin]       = useState('')
  const [showStdin,   setShowStdin]   = useState(false)
  const [output,      setOutput]      = useState(null)   // string | null
  const [errorInfo,   setErrorInfo]   = useState(null)   // {type,msg,line,col} | null
  const [loading,     setLoading]     = useState(false)
  const gutterRef   = useRef(null)

  const run = useCallback(async () => {
    setLoading(true)
    setOutput(null)
    setErrorInfo(null)
    try {
      const res = await fetch('/api/run', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ code, stdin }),
      })
      const data = await res.json()
      if (data.errorType) {
        setErrorInfo({ type: data.errorType, msg: data.error, line: data.line, col: data.col })
      } else {
        setOutput(data.output)
      }
    } catch (err) {
      setErrorInfo({ type: 'Internal Error', msg: 'Could not reach the LEXOR server. Is it running on port 8080?', line: 0, col: 0 })
    } finally {
      setLoading(false)
    }
  }, [code, stdin])

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      run()
    }
    if (e.key === 'Tab') {
      e.preventDefault()
      const { selectionStart: s, selectionEnd: end, value } = e.target
      const next = value.slice(0, s) + '    ' + value.slice(end)
      setCode(next)
      requestAnimationFrame(() => {
        e.target.selectionStart = e.target.selectionEnd = s + 4
      })
    }
  }

  return (
    <>
      <style>{`
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { margin: 0; background: ${COLORS.bg}; }
        @keyframes spin { to { transform: rotate(360deg); } }
        textarea::-webkit-scrollbar, .output-area::-webkit-scrollbar { width: 6px; height: 6px; }
        textarea::-webkit-scrollbar-track, .output-area::-webkit-scrollbar-track { background: transparent; }
        textarea::-webkit-scrollbar-thumb, .output-area::-webkit-scrollbar-thumb { background: ${COLORS.border}; border-radius: 3px; }
        .line-gutter::-webkit-scrollbar { display: none; }
        .line-gutter { scrollbar-width: none; }
      `}</style>

      <div style={s.root}>

        {/* ── Header ── */}
        <div style={s.header}>
          <h1 style={s.headerTitle}>LEXOR</h1>
          <span style={{ color: COLORS.muted, fontSize: 13 }}>Web IDE</span>
          <span style={s.headerSub}>Ctrl+Enter to run</span>
        </div>

        {/* ── Editor + Output ── */}
        <div style={s.body}>

          {/* Left: code editor */}
          <div style={s.pane}>
            <div style={s.paneLabel}>Editor</div>
            <div style={s.editorWrap}>
              <div
                ref={gutterRef}
                className="line-gutter"
                style={s.lineNumbers}
              >
                {code.split('\n').map((_, i) => (
                  <div key={i}>{i + 1}</div>
                ))}
              </div>
              <textarea
                style={s.textarea}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                onKeyDown={handleKeyDown}
                onScroll={(e) => {
                  if (gutterRef.current)
                    gutterRef.current.scrollTop = e.target.scrollTop
                }}
                spellCheck={false}
                autoCorrect="off"
                autoCapitalize="off"
                placeholder="Type your LEXOR program here…"
              />
            </div>
          </div>

          <div style={s.divider} />

          {/* Right: output panel */}
          <div style={s.pane}>
            <div style={s.paneLabel}>Output</div>
            <div className="output-area" style={s.outputArea}>
              {loading && (
                <span style={{ color: COLORS.muted }}>
                  <span style={s.spinner} />Running…
                </span>
              )}
              {!loading && errorInfo && (
                <div>
                  <div style={{ marginBottom: 8 }}>
                    <ErrorBadge type={errorInfo.type} />
                    {errorInfo.line > 0 && (
                      <span style={{ color: COLORS.muted, fontSize: 12 }}>
                        line {errorInfo.line}{errorInfo.col > 0 ? `, col ${errorInfo.col}` : ''}
                      </span>
                    )}
                  </div>
                  <span style={{ color: COLORS.red }}>{errorInfo.msg}</span>
                </div>
              )}
              {!loading && output !== null && (
                output === ''
                  ? <span style={s.placeholder}>(no output)</span>
                  : <span style={{ color: COLORS.green }}>{output}</span>
              )}
              {!loading && output === null && !errorInfo && (
                <span style={s.placeholder}>Run your program to see output here.</span>
              )}
            </div>
          </div>

        </div>

        {/* ── SCAN stdin input (collapsible) ── */}
        {showStdin && (
          <div style={s.stdinWrap}>
            <span style={s.stdinLabel}>SCAN input (comma-separated values, one line per SCAN statement)</span>
            <input
              type="text"
              style={s.stdinInput}
              value={stdin}
              onChange={(e) => setStdin(e.target.value)}
              placeholder="e.g.  42,TRUE,z"
            />
          </div>
        )}

        {/* ── Toolbar ── */}
        <div style={s.toolbar}>
          <button
            style={s.btn('secondary')}
            onClick={() => setShowStdin((v) => !v)}
          >
            {showStdin ? 'Hide' : 'Show'} SCAN Input
          </button>
          <button
            style={s.btn('run')}
            onClick={run}
            disabled={loading}
          >
            {loading && <span style={s.spinner} />}
            {loading ? 'Running…' : '▶  Run'}
          </button>
        </div>

      </div>
    </>
  )
}

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const PUBLIC_DIR = path.resolve(__dirname, '..');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webmanifest': 'application/manifest+json',
  '.map': 'application/json'
};

const server = http.createServer((req, res) => {
  try {
    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    let pathname = decodeURIComponent(parsedUrl.pathname);

    if (pathname === '/' || pathname === '') {
      pathname = '/index.html';
    }

    let filePath = path.join(PUBLIC_DIR, pathname);

    // Proteção de segurança contra navegação em diretório
    if (!filePath.startsWith(PUBLIC_DIR)) {
      res.writeHead(403, { 'Content-Type': 'text/plain' });
      return res.end('403 Forbidden');
    }

    fs.stat(filePath, (err, stats) => {
      if (err || !stats.isFile()) {
        // Fallback para SPA e PWA
        filePath = path.join(PUBLIC_DIR, 'index.html');
      }

      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';

      const headers = {
        'Content-Type': contentType,
        'Access-Control-Allow-Origin': '*'
      };

      if (filePath.endsWith('sw.js')) {
        headers['Service-Worker-Allowed'] = '/';
        headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
      }

      fs.readFile(filePath, (readErr, content) => {
        if (readErr) {
          res.writeHead(500, { 'Content-Type': 'text/plain' });
          return res.end('500 Server Error');
        }
        res.writeHead(200, headers);
        res.end(content);
      });
    });
  } catch (ex) {
    res.writeHead(500, { 'Content-Type': 'text/plain' });
    res.end('500 Error: ' + ex.message);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[PWA Server] Sistema Foco na Prevenção SST rodando em http://0.0.0.0:${PORT}`);
});

/**
 * Express server for snapshot review dashboard
 */

import express from 'express';
import * as path from 'path';
import * as fs from 'fs/promises';
import testRootsRouter from './routes/test-roots';
import approveRouter from './routes/approve';
import rejectRouter from './routes/reject';

const app = express();
const PORT = process.env.PORT || 3000;

/**
 * Validate environment configuration on startup
 */
async function validateEnvironment(): Promise<void> {
  // Check EXAMPLES_DIR is set
  if (!process.env.EXAMPLES_DIR) {
    console.error('ERROR: EXAMPLES_DIR environment variable is not set');
    console.error('Please set EXAMPLES_DIR to the absolute path of your snapshots directory');
    console.error('Example: export EXAMPLES_DIR=/Users/me/work/data-api-tools/resources/mock_examples');
    process.exit(1);
  }

  const examplesDir = process.env.EXAMPLES_DIR;

  // Check EXAMPLES_DIR exists
  try {
    const stat = await fs.stat(examplesDir);
    if (!stat.isDirectory()) {
      console.error(`ERROR: EXAMPLES_DIR is not a directory: ${examplesDir}`);
      process.exit(1);
    }
  } catch (error) {
    console.error(`ERROR: EXAMPLES_DIR does not exist: ${examplesDir}`);
    process.exit(1);
  }

  console.log('✓ Environment validation passed');
  console.log(`  EXAMPLES_DIR: ${examplesDir}`);
  console.log(`  PORT: ${PORT}`);
}

/**
 * Configure middleware
 */
function configureMiddleware(): void {
  // Parse JSON bodies
  app.use(express.json());

  // Serve static files from public directory
  app.use(express.static(path.join(__dirname, '../public')));

  // Log requests in development
  if (process.env.NODE_ENV !== 'production') {
    app.use((req, res, next) => {
      console.log(`${req.method} ${req.path}`);
      next();
    });
  }
}

/**
 * Configure routes
 */
function configureRoutes(): void {
  app.use('/api/test-roots', testRootsRouter);
  app.use('/api/approve', approveRouter);
  app.use('/api/reject', rejectRouter);

  // Health check endpoint
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // Serve index.html for all other routes (SPA fallback)
  app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '../public/index.html'));
  });
}

/**
 * Start the server
 */
async function start(): Promise<void> {
  try {
    // Validate environment first
    await validateEnvironment();

    // Configure app
    configureMiddleware();
    configureRoutes();

    // Start listening
    app.listen(PORT, () => {
      console.log('');
      console.log('='.repeat(60));
      console.log('  Snapshot Review Dashboard');
      console.log('='.repeat(60));
      console.log(`  Server running at: http://localhost:${PORT}`);
      console.log(`  Examples directory: ${process.env.EXAMPLES_DIR}`);
      console.log('='.repeat(60));
      console.log('');
      console.log('Press Ctrl+C to stop the server');
      console.log('');
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Start the server
start();

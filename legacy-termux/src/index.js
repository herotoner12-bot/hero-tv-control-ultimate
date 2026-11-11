const express = require('express');
const path = require('path');
const { execFile } = require('child_process');
const { getCommand } = require('./commands');

const app = express();
app.use(express.json({ limit: '2kb' }));
app.use(express.static(path.join(__dirname, '..', 'public')));

app.get('/status', (req, res) => {
  res.json({
    project: 'HERO TV CONTROL',
    status: 'running'
  });
});

app.post('/command', (req, res) => {
  try {
    const key = getCommand(req.body.command);

    execFile('adb', [
      'shell', 'input', 'keyevent', String(key)
    ], { timeout: 5000 }, (error) => {
      if (error) {
        return res.status(503).json({
          success: false,
          message: 'تعذر تنفيذ الأمر. تحقق من اتصال التلفزيون.'
        });
      }

      res.json({
        success: true,
        command: req.body.command
      });
    });
  } catch {
    res.status(400).json({
      success: false,
      message: 'الأمر غير مسموح'
    });
  }
});

app.listen(3001, '127.0.0.1', () => {
  console.log('HERO TV CONTROL ready on port 3001');
});

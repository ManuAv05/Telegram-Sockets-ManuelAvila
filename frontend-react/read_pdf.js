import fs from 'fs';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const pdfModule = require('pdf-parse');

const pdfParse = typeof pdfModule === 'function' ? pdfModule : pdfModule.default;

const buffer = fs.readFileSync('../Practica 2 - Sockets.pdf');
pdfParse(buffer).then(data => {
  console.log(data.text.substring(0, 4000));
}).catch(console.error);

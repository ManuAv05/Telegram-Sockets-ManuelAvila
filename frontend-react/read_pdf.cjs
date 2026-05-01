const fs = require('fs');
const pdfParse = require('pdf-parse/lib/pdf-parse.js');

const buffer = fs.readFileSync('../Practica 2 - Sockets.pdf');
pdfParse(buffer).then(data => {
  console.log(data.text.substring(0, 4000));
}).catch(console.error);

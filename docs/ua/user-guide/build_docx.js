// Builds «Посібник Renault Docs» as Word (.docx) from guide.json (export_assets.py).
// Usage: node build_docx.js <guide.json> <out.docx>
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, ImageRun, AlignmentType,
  LevelFormat, Footer, PageNumber, ShadingType, BorderStyle, PageBreak, LineRuleType,
} = require("docx");

const [, , jsonPath, outPath] = process.argv;
const guide = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

const FONT = "DejaVu Sans";
const ACCENT = "1F5FA8";
const MUTED = "566476";
const WARN = "8A5A12";
const WARN_BG = "FBF1DF";
const CONTENT_WIDTH_PX = 600; // ~16 cm at 96 dpi on A4 with 2 cm margins

// «<b>…</b>» in the source → bold runs.
function runs(text, extra = {}) {
  return text.split(/(<b>.*?<\/b>)/).filter(Boolean).map((part) => {
    const bold = part.startsWith("<b>");
    return new TextRun({ text: bold ? part.slice(3, -4) : part, bold, font: FONT, ...extra });
  });
}

function pngSize(file) {
  const buf = fs.readFileSync(file);
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20), data: buf };
}

function image(file) {
  const { w, h, data } = pngSize(file);
  // Graphviz renders at 200 dpi; show at natural size but never wider than the page.
  let width = Math.round((w * 96) / 200);
  let height = Math.round((h * 96) / 200);
  if (width > CONTENT_WIDTH_PX) {
    height = Math.round((height * CONTENT_WIDTH_PX) / width);
    width = CONTENT_WIDTH_PX;
  }
  const MAX_H = 720;
  if (height > MAX_H) {
    width = Math.round((width * MAX_H) / height);
    height = MAX_H;
  }
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    // Single auto line spacing: an "exact" line height would crop the picture.
    spacing: { before: 120, after: 160, line: 240, lineRule: LineRuleType.AUTO },
    children: [new ImageRun({ type: "png", data, transformation: { width, height } })],
  });
}

const children = [
  new Paragraph({
    spacing: { after: 80 },
    children: [new TextRun({ text: `RENAULT DOCS · ${guide.meta.version} · ${guide.meta.date}`, font: FONT, size: 18, color: MUTED })],
  }),
  new Paragraph({ heading: HeadingLevel.TITLE, children: [new TextRun({ text: "Посібник Renault Docs", font: FONT })] }),
  new Paragraph({
    spacing: { after: 240 },
    children: runs("Що вміє застосунок і через які кнопки. Кожен розділ має схему: стрілки показують, куди веде натискання.", { color: MUTED }),
  }),
  new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: "Зміст", font: FONT })] }),
  ...guide.sections.map((s, i) => new Paragraph({
    numbering: { reference: "toc", level: 0 },
    children: [new TextRun({ text: s.title, font: FONT })],
  })),
];

guide.sections.forEach((s) => {
  children.push(new Paragraph({ children: [new PageBreak()] }));
  children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: s.title, font: FONT })] }));
  s.blocks.forEach((b) => {
    if (b.kind === "p") {
      children.push(new Paragraph({ spacing: { after: 140 }, children: runs(b.value) }));
    } else if (b.kind === "ul") {
      b.value.forEach((item) => children.push(new Paragraph({
        numbering: { reference: "bullets", level: 0 },
        spacing: { after: 80 },
        children: runs(item),
      })));
    } else if (b.kind === "note") {
      children.push(new Paragraph({
        spacing: { before: 120, after: 160 },
        shading: { type: ShadingType.CLEAR, color: "auto", fill: WARN_BG },
        border: { left: { style: BorderStyle.SINGLE, size: 18, color: WARN, space: 8 } },
        children: runs(b.value, { color: WARN, bold: true }),
      }));
    } else if (b.kind === "image") {
      children.push(image(b.path));
    }
  });
});

children.push(new Paragraph({
  spacing: { before: 360 },
  children: runs(`Посібник зібрано з коду застосунку ${guide.meta.version}. Якщо кнопка на телефоні називається інакше — правда за телефоном.`, { color: MUTED, size: 18 }),
}));

const doc = new Document({
  creator: "Renault Docs",
  title: "Посібник Renault Docs",
  styles: {
    default: { document: { run: { font: FONT, size: 22 }, paragraph: { spacing: { line: 300, lineRule: LineRuleType.AUTO } } } },
    paragraphStyles: [
      { id: "Title", name: "Title", basedOn: "Normal", run: { size: 48, bold: true, color: "16202C", font: FONT }, paragraph: { spacing: { after: 120 } } },
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", run: { size: 34, bold: true, color: ACCENT, font: FONT }, paragraph: { spacing: { before: 120, after: 160 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", run: { size: 26, bold: true, color: "16202C", font: FONT }, paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 1 } },
    ],
  },
  numbering: {
    config: [
      { reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] },
      { reference: "toc", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 360 } } } }] },
    ],
  },
  sections: [{
    properties: { page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ font: FONT, size: 16, color: MUTED, children: ["Посібник Renault Docs · ", PageNumber.CURRENT] })],
        })],
      }),
    },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(outPath, buf);
  console.log("written", path.basename(outPath), buf.length, "bytes");
});

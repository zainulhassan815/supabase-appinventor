const fs = require('fs');
const os = require('os');
const path = require('path');
const puppeteer = require('puppeteer');

const USAGE = 'usage: compile.js <appinventor-dir> <Screen.scm> <Screen.bky> <components.json> <screen-class> <out.yail> [out.bky]';
const YA_VERSION = '237';
const BLOCKS_VERSION = '39';
const SCRIPTS = [
  'build/blocklyeditor/msg/messages.nocache.js',
  'build/blocklyeditor/blockly-all.js',
  'build/blocklyeditor/component-types.js',
  'appengine/war/static/js/scroll-options-6.0.16.min.js',
  'appengine/war/static/js/workspace-search-9.1.10.min.js',
  'appengine/war/static/js/block-dynamic-connection-0.7.16.min.js',
  'appengine/war/static/js/blockly-field-colour-5.0.19.min.js',
  'appengine/war/static/js/workspace-multiselect-pr135-edf5dd2.min.js',
  'blocklyeditor/tests/testCommon.js',
  'blocklyeditor/src/msg/en/_messages.js',
];

function page(appinventor) {
  const scripts = SCRIPTS.map(s => `<script src="file://${path.join(appinventor, s)}"></script>`).join('\n');
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'blocks-')), 'page.html');
  fs.writeFileSync(file, `<!doctype html><html><head><meta charset="utf-8">${scripts}</head><body></body></html>`);
  return file;
}

function compile(form, bky, extensions, packageName, versions) {
  window.YA_VERSION = versions.ya;
  window.BLOCKS_VERSION = versions.blocks;
  componentTypeJson.push(...extensions);
  const workspace = Blockly.BlocklyEditor.create(document.body, 'Screen1', false, false);
  Blockly.common.setMainWorkspace(workspace);
  initComponentTypes();
  processVersion(versions.blocks, versions.ya);
  processForm(form);
  processBlocks(form, bky);
  workspace.cleanUp();
  return {
    yail: AI.Yail.getFormYail(form, packageName, false, workspace),
    bky: Blockly.SaveFile.get(true, workspace),
    bad: workspace.getAllBlocks(false).filter(b => b.isBadBlock && b.isBadBlock()).map(b => b.type),
  };
}

async function main() {
  const [appinventor, scmPath, bkyPath, extensionsPath, packageName, outYail, outBky] = process.argv.slice(2);
  if (!outYail) {
    console.error(USAGE);
    process.exit(2);
  }
  const scm = fs.readFileSync(scmPath, 'utf8');
  const form = scm.substring(scm.indexOf('{'), scm.lastIndexOf('}') + 1);
  const extensions = JSON.parse(fs.readFileSync(extensionsPath, 'utf8'));
  const browser = await puppeteer.launch({ args: ['--allow-file-access-from-files', '--no-sandbox'] });
  const tab = await browser.newPage();
  const errors = [];
  tab.on('pageerror', e => errors.push(e.message));
  await tab.goto('file://' + page(appinventor));
  const result = await tab.evaluate(compile, form, fs.readFileSync(bkyPath, 'utf8'), extensions, packageName,
    { ya: YA_VERSION, blocks: BLOCKS_VERSION });
  await browser.close();
  if (errors.length || result.bad.length) {
    console.error([...errors, ...result.bad.map(b => `bad block: ${b}`)].join('\n'));
    process.exit(1);
  }
  fs.writeFileSync(outYail, result.yail);
  if (outBky) fs.writeFileSync(outBky, result.bky);
}

main();

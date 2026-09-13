import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';

const highlight = function (str, lang) {
  if (lang && hljs.getLanguage(lang)) {
    try {
      return hljs.highlight(str, { language: lang }).value;
    } catch (__) {}
  }
  return hljs.highlightAuto(str).value;
};

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight
});

// 用户消息专用渲染实例：html 设为 false，markdown-it 会自动转义原生 HTML，
// 在支持 Markdown 渲染的同时防止用户输入注入 XSS
const mdUser = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: false,
  breaks: true,
  highlight
});

export default md;
export { md, mdUser };

export function renderMarkdown(content) {
  if (!content) return '';
  let text = content;
  text = text.replace(/\\n/g, '\n');
  text = text.replace(/\\r/g, '\r');
  
  const codeBlockCount = (text.match(/```/g) || []).length;
  if (codeBlockCount % 2 !== 0) {
    text += '\n```';
  }
  
  return md.render(text);
}
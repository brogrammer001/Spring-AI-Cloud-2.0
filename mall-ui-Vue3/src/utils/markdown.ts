import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value;
      } catch (__) {}
    }
    return hljs.highlightAuto(str).value;
  }
});

export default md;
export { md };

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
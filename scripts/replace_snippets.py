import os
import re
import urllib.request
import sys
import html

def replace_snippets(content):
    def get_snippet(match):
        url = match.group(1)
        line_range = match.group(2)
        try:
            with urllib.request.urlopen(url) as response:
                lines = response.read().decode("utf-8").splitlines()
                if line_range:
                    if line_range.endswith("-"):
                        lines = lines[int(line_range[:-1]) - 1:]
                    elif "-" in line_range:
                        start, end = map(int, line_range.split("-"))
                        lines = lines[start-1:end]
                    else:
                        lines = [lines[int(line_range)-1]]
                snippet_text = "\n".join(lines)
                # Escape HTML characters so they are rendered literally in the browser
                return html.escape(snippet_text)
        except Exception as e:
            print(f"Error fetching {url}: {e}")
            return match.group(0)
            
    pattern = re.compile(r"\{snippet:\s*(https?://[^\s}]+)(?:\s+([0-9-]+))?\s*\}")
    return pattern.sub(get_snippet, content)

def main():
    site_dir = sys.argv[1] if len(sys.argv) > 1 else "/tmp/generated-site"
    for root, _, files in os.walk(site_dir):
        for file in files:
            if file.endswith(".html"):
                path = os.path.join(root, file)
                with open(path, "r", encoding="utf-8") as f:
                    content = f.read()
                new_content = replace_snippets(content)
                if new_content != content:
                    with open(path, "w", encoding="utf-8") as f:
                        f.write(new_content)

if __name__ == "__main__":
    main()

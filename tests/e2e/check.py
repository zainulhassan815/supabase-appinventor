import re
import sys

expected = [line for line in open(sys.argv[1]).read().splitlines() if line]
actual = [line for line in sys.stdin.read().splitlines() if line]
failures = [f'line {i + 1}: expected /{pattern}/, got {actual[i] if i < len(actual) else "nothing"!r}'
            for i, pattern in enumerate(expected) if i >= len(actual) or not re.fullmatch(pattern, actual[i])]
if len(actual) > len(expected):
    failures.append(f'unexpected lines: {actual[len(expected):]}')
print('\n'.join(actual))
if failures:
    sys.exit('FAIL\n' + '\n'.join(failures))
print('PASS')

import sys
import pathlib

url = str(sys.argv[1])
# print('{"key": "' + url.split('\\')[-1].split('.')[0] + '", "result": "' + pathlib.Path(url).as_uri() + '"}')
print(url.split('\\')[-1] + ',classification')
import requests
import json
import os

def send_to_ocr():
    url = "http://127.0.0.1:8000/upload"
    filename = 'fox.jpg'
    files = {'file': (os.path.basename(filename), open(filename, 'rb'), 'application/octet-stream')}
    res = requests.post(url, files=files)

    print("Status Code", res.status_code)
    print("Response ", json.loads(res.text))

    return res.text

if __name__ == '__main__':
    send_to_ocr()
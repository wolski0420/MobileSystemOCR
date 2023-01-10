# Mobile System OCR

### Datasets for OCR

If we want to focus on OCR results (to say that we have
correct text at output), we could use one of these datasets:

- https://www.kaggle.com/datasets/sthabile/noisy-and-rotated-scanned-documents
- https://www.kaggle.com/datasets/urbikn/sroie-datasetv2

They both contain not only images but also expected OCR result (text/rotation angle etc.)

If we do not want to focus on OCR results, it should be sufficient:

- https://github.com/clovaai/cord

It contains JSON files with not really expected results (categorized).

Mobile ocr done with
https://www.codeproject.com/Articles/1275580/Android-OCR-Application-Based-on-Tesseract

ocr-cloud-app

Flask app created with tessaract and opencv.

To run server navigate to 'cloud-ocr-app' and run 'docker compose up'.
You can acces it at http://127.0.0.1:8000/
To perform tests navigate to 'cloud-ocr-app' and run python -m unittest discover -s test -p '*test.py'

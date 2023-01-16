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


Aspekty bezpieczeństwa się można sprwadzic do 3 atrybutów informacji pofuności, integralności oraz ich dostępności.
Danymi w naszej aplikacji są wyniki z algorytmu ocr.
Rozpatrując aspekt przsyłanie zdjęcia do serwer dane atrybuty można zdfiniować jako:
### - dostępność - czas po którym otrzyamy dostęp do danych
### - integralość - otrzymane dane zostały uzyskane z wykonania algorytmu ocr na identycznym zdjęciu i otrzymane dane są identyczne do tych które wysłał serwer 
### - poufność - tylko my możemy poznać otrzymane dane

### Integralność danych
Moze być naruszona poprzez przypadkowy bład podczas wysyłania lub przez zamierzoną ingerecje w przesyłąne dane przez osobę trzecią.
### Pounfność 
Naruszona przez osobę trzecia podczas przesyłu danych (wysyłanie i odbieranie). Serwer jest traktowany jako bezpieczny element.
Skala:  0 - brak zabepieczeń, 1 - jest zabezpieczenie
Integralność - Przypadkowy bład, Integralność - Przypadkowy bład, Integralność - Ingerencja, Punfność - Ingerencja wysyłąnie, Punfność -  Ingerencja Odbieranie

1) Podstawowe wysłanie - 0,0,0,0
2) Dodanie cheksum - 1,0,0,0
3) szyfrowanie klient-serwer - 0,0,1,0
4) szyfrowanie serwer-klient - 0,0,0,1
5) szyfrowanie klient-serwer-klient - 0,0,1,1
6) checksumy szyfrowane klient-serwer-klient + dane surowe - 1,1,0,0
7)szyforawnie cheksum i danych powrotnych, plik bez szyforawnie(duzo danych) = 1,1,0,1
8) checksumy i dane szyfrowane klient-serwer-klient - 1,1,1,1

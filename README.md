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
7) szyforawnie cheksum i danych powrotnych, plik bez szyforawnie(duzo danych) = 1,1,0,1
8) checksumy i dane szyfrowane klient-serwer-klient - 1,1,1,1

***
## Linear regression models - local and cloud
Models are being trained with use of data stored from tests (local from local tests, cloud from cloud tests.)
Serialized models are saved as a pickle files (cloud_model/local_model.pkl).
With use of pickle, models can be deserialized in any other script.

Model learning

Time, uploadBandwidth, downloadBandwidth are used for linear regression models.
They predict estimated time of ocr process for local and cloud separately.
In the next step all parameters:

- time_ratio (local_time/cloud_time)
- battery (current state)
- ram (current state)
- safetyLevel (1-8)

are being assessed on a scale 2-5:

- time_ratio (weight = 0.35)
    - 5 points if time_ratio < 1
    - 4 points if 1.5 > time_ratio >= 1
    - 3 points if 2 > time_ratio >= 1.5
    - 2 points if time_ratio >= 2

- battery (weight = 0.1)
    - 5 points if battery > 75
    - 4 points if 75 >= battery > 50
    - 3 points if 50 >= battery > 25
    - 2 points if 25 >= battery

- ram (weight = 0.1)
    - 2 points if ram > 75
    - 3 points if 75 >= ram > 50
    - 4 points if 50 >= ram > 25
    - 5 points if 25 >= ram

- safetyLevel (weight = 0.45)
    - 5 points if safetyLevel = 8 or 7
    - 4 points if safetyLevel = 6 or 5
    - 3 points if safetyLevel = 4 or 3
    - 2 points if safetyLevel = 2 or 1

Generally higher points value indicates performing ocr on a local machine.

In the last step weighted average is being calculated.

    return round(sum([ax[i]*weights[i] for i in range(len(ax))])/sum(weights), 1) * 20

Fuction returns percentage value (assessment/5).

If assessment >= 60 % -> ocr should be performed locally.
If assessment < 60 % -> ocr should be performed on cloud.

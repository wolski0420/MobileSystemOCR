FROM ubuntu:18.04

RUN apt-get update \
  && apt-get -y install tesseract-ocr \
  && apt-get install -y python3 python3-distutils python3-pip \
  && cd /usr/local/bin \
  && ln -s /usr/bin/python3 python \
  && pip3 --no-cache-dir install --upgrade pip setuptools wheel \
  && rm -rf /var/lib/apt/lists/*

RUN apt update \
  && apt-get install ffmpeg libsm6 libxext6 -y

COPY ./requirements.txt /requirements.txt
RUN pip install -r requirements.txt

COPY ./app /app
WORKDIR /app

ENTRYPOINT ["python3"]

CMD ["python", "-m", "flask", "run", "--host=0.0.0.0", "--port=8000"]
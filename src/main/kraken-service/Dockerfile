FROM python:3.10-slim

# Cài đặt các thư viện hệ thống cần thiết cho Kraken và PyVips
RUN apt-get update && apt-get install -y \
    libvips-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 8000

CMD ["python", "app.py"]
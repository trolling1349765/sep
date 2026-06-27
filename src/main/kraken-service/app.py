from fastapi import FastAPI, UploadFile, File
import uvicorn
from kraken import binarization, pageseg
from PIL import Image
import io

app = FastAPI()

@app.post("/ocr")
async def do_ocr(file: UploadFile = File(...)):
    # Read image from request
    request_object_content = await file.read()
    img = Image.open(io.BytesIO(request_object_content))

    # 1. Binarization (Chuyển ảnh về dạng trắng đen để OCR chuẩn hơn)
    bw_img = binarization.nlbin(img)

    # 2. Segment (Cắt dòng chữ viết tay)
    seg_result = pageseg.segment(bw_img)

    # 3. Phân đoạn dòng (Segment)
    seg_result = pageseg.segment(bw_img)

    # Đoạn bọc lót thông minh để check kiểu dữ liệu trả về của Kraken
    lines_count = 0
    if hasattr(seg_result, 'lines'):
        lines_count = len(seg_result.lines)
    elif isinstance(seg_result, dict) and 'lines' in seg_result:
        lines_count = len(seg_result['lines'])
    elif hasattr(seg_result, 'boxes'):
        lines_count = len(seg_result.boxes)
    else:
        # Nếu là một list/iterable thông thường
        try:
            lines_count = len(seg_result)
        except:
            lines_count = -1 # Không đếm được theo cách thông thường

    return {
        "status": "Success",
        "lines_detected": lines_count
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
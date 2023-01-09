import unittest
from call import send_to_ocr

class Test(unittest.TestCase):
    def test_ocr(self):
        result = send_to_ocr()
        print(result)

        expected_result = '"Process finished succesfully!. OCR result: The quick brown fox\\njumped over the 5\\nlazy dogs!\\n\\f"'

        self.assertEqual(result, expected_result)

# python -m unittest discover -s test -p '*test.py'
import pickle
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType, Int64TensorType
import numpy as np
"""Model training with use of train_model function.
Next model is being serialized with use of pickle and saved to a file.
Therefore model can be deserialized in any other script."""

with open('local_model.pkl', 'rb') as f:
    local_model = pickle.loads(f.read())

with open('cloud_model.pkl', 'rb') as f:
    cloud_model = pickle.loads( f.read())

print(local_model.predict([[110502.0 ,3.102375, 1.875]]))

initial_type = [('float_input', FloatTensorType([None,3]))]
converted_local = convert_sklearn( local_model , initial_types=initial_type )


with open('local_model.onnx', 'wb') as f:
     f.write( converted_local.SerializeToString() )

    
converted_cloud = convert_sklearn( cloud_model , initial_types=initial_type )
    
with open('cloud_model.onnx', 'wb') as f:
    f.write( converted_cloud.SerializeToString() )


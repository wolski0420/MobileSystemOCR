from training import train_model
import pickle

"""Model training with use of train_model function.
Next model is being serialized with use of pickle and saved to a file.
Therefore model can be deserialized in any other script."""

local_model = train_model('decision-model/data/local.csv')
cloud_model = train_model('decision-model/data/fullcloud.csv')

with open('decision-model/local_model.pkl', 'wb') as f:
    pickle.dump(local_model, f)

with open('decision-model/cloud_model.pkl', 'wb') as f:
    pickle.dump(cloud_model, f)

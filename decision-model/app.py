from utils.prediction import decision
import joblib

if __name__ == '__main__':
    """Deserializing model file and using it for decision."""
    local_model = joblib.load(filename='decision-model/local_model.pkl')
    cloud_model = joblib.load(filename='decision-model/cloud_model.pkl')

    print(decision(700000, 90.0, 90.0, 25, 85, 4, local_model, cloud_model))
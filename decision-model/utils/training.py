import pandas as pd
from sklearn.linear_model import LinearRegression
import numpy as np
from sklearn.model_selection import train_test_split

def train_model(data: str):
    """Linear regression training model."""
    df = pd.read_csv(data)

    df = df.drop(['Battery', 'RAM', 'NetworkType'], axis=1)

    y = df.pop('Time')
    x = df

    x_array = np.array(x)
    y_array = np.array(y)

    x_train, x_test, y_train, y_test = train_test_split(x_array, y_array, test_size=0.3)

    lr_model = LinearRegression()
    lr_model.fit(x_train, y_train)

    return lr_model
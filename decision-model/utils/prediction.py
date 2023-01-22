import numpy as np

def prediction(size: int, downloadBandwidth: float, uploadBandwidth: float, model) -> float:
    """Predicting estimating time of OCR process for given parameters."""

    time = model.predict(np.array([size, downloadBandwidth, uploadBandwidth]).reshape(1, 3))

    return time / 1000

def assessment(time_ratio: float, battery:int, ram: float, safetyLevel: int) -> float:
    """Calculating formula based on received parameters."""
    match time_ratio:
        case _ if time_ratio < 1:
            time_ax = 5
        case _ if time_ratio >= 1 and time_ratio < 1.5:
            time_ax = 4
        case _ if time_ratio >= 1.5 and time_ratio < 2:
            time_ax = 3
        case _ if time_ratio >= 2:
            time_ax = 2

    match battery:
        case _ if battery > 75 and battery <= 100:
            battery_ax = 5
        case _ if battery > 50 and battery <= 75:
            battery_ax = 4
        case _ if battery > 25 and battery <= 50:
            battery_ax = 3
        case _ if battery <= 25:
            battery_ax = 2

    match ram:
        case _ if ram > 75 and ram <= 100:
            ram_ax = 2
        case _ if ram > 50 and ram <= 75:
            ram_ax = 3
        case _ if ram > 25 and ram <= 50:
            ram_ax = 4
        case _ if ram <= 25:
            ram_ax = 5

    match safetyLevel:
        case _ if safetyLevel == 8 or safetyLevel == 7:
            safetyLevel_ax = 5
        case _ if safetyLevel == 6 or safetyLevel == 5:
            safetyLevel_ax = 4
        case _ if safetyLevel == 4 or safetyLevel == 3:
            safetyLevel_ax = 3
        case _ if safetyLevel == 2 or safetyLevel == 1:
            safetyLevel_ax = 2

    ax = []
    ax.extend([time_ax, battery_ax, ram_ax, safetyLevel_ax])
    weights = [0.35, 0.1, 0.1, 0.45]

    return round(sum([ax[i]*weights[i] for i in range(len(ax))])/sum(weights), 1) * 20

def decision(size: int, downloadBandwidth: float, uploadBandwidth: float, battery: int, ram: float, safetyLevel: int, local_model, cloud_model) -> tuple:
    """Deciding if OCR should be processed locally or send to the cloud."""

    local_time = prediction(size, downloadBandwidth, uploadBandwidth, local_model)
    cloud_time = prediction(size, downloadBandwidth, uploadBandwidth, cloud_model)
    print('local time', local_time)
    print('cloud time', cloud_time)

    ax = assessment(local_time/cloud_time, battery, ram, safetyLevel)

    if ax >= 60:
        return('local', ax)
    else:
        return('cloud', ax)

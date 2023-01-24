import matplotlib.pyplot as plt
import csv


mappings = {
    "results/b20r51nMobile.csv": "Battery low, RAM minimal, Mobile networking",
    "results/b20r51nWifi.csv": "Battery low, RAM minimal, Wifi networking",
    "results/b20r85nMobile.csv": "Battery low, RAM overload, Mobile networking",
    "results/b20r85nWifi.csv": "Battery low, RAM overload, Wifi networking",
    "results/b44r52nMobile.csv": "Battery medium, RAM minimal, Mobile networking",
    "results/b44r52nWifi.csv": "Battery medium, RAM minimal, Wifi networking",
    "results/b44r85nMobile.csv": "Battery medium, RAM overload, Mobile networking",
    "results/b44r85nWifi.csv": "Battery medium, RAM overload, Wifi networking",
    "results/b100r52nMobile.csv": "Battery full, RAM minimal, Mobile networking",
    "results/b100r51nWifi.csv": "Battery full, RAM minimal, Wifi networking",
    "results/b100r85nMobile.csv": "Battery full, RAM overload, Mobile networking",
    "results/b100r85nWifi.csv": "Battery full, RAM overload, Wifi networking",
}

for filename, description in mappings.items():
    decisions, predictions = [], []
    with open(filename, 'r') as csvfile:
        data = csv.reader(csvfile, delimiter=",")
        next(csvfile)
        for row in data:
            decisions.append("Cloud" if row[0] == "true" else "Local")
            predictions.append(float(row[1]))

    strong, weak = {"Cloud": 0, "Local": 0}, {"Cloud": 0, "Local": 0}
    for decision, prediction in zip(decisions, predictions):
        if prediction < 55 or 65 < prediction:
            strong[decision] = strong[decision] + 1
        else:
            weak[decision] = weak[decision] + 1

    print(strong.values())
    plt.bar(strong.keys(), strong.values(), color="g", width=0.4, label="Strong choices")
    plt.bar(weak.keys(), weak.values(), bottom=[*strong.values()], color="r", width=0.4, label="Weak choices")
    plt.xlabel("Final decision")
    plt.ylabel("Number of choices")
    plt.yticks(range(0, 9, 1))
    plt.title(description)
    plt.legend()
    plt.savefig("images/" + filename.split("/")[1].split(".")[0] + ".png")
    # plt.show()

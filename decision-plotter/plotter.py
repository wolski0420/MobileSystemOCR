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

all_decisions_occurrences = {
    1: {"Cloud": 0, "Local": 0},
    2: {"Cloud": 0, "Local": 0},
    3: {"Cloud": 0, "Local": 0},
    4: {"Cloud": 0, "Local": 0},
    5: {"Cloud": 0, "Local": 0},
    6: {"Cloud": 0, "Local": 0},
    7: {"Cloud": 0, "Local": 0},
    8: {"Cloud": 0, "Local": 0},
}

for filename, description in mappings.items():
    # taking from file
    decisions, predictions = [], []
    with open(filename, 'r') as csvfile:
        data = csv.reader(csvfile, delimiter=",")
        next(csvfile)
        for row in data:
            decisions.append("Cloud" if row[0] == "true" else "Local")
            predictions.append(float(row[1]))

    # counting all occurrences for histogram
    for i in range(len(decisions)):
        all_decisions_occurrences[i+1][decisions[i]] += 1

    # counting weak and strong choices for given experiment
    strong, weak = {"Cloud": 0, "Local": 0}, {"Cloud": 0, "Local": 0}
    for decision, prediction in zip(decisions, predictions):
        if prediction < 55 or 65 < prediction:
            strong[decision] = strong[decision] + 1
        else:
            weak[decision] = weak[decision] + 1

    # plotting results for given experiment
    plt.bar(strong.keys(), strong.values(), color="g", width=0.4, label="Strong choices")
    plt.bar(weak.keys(), weak.values(), bottom=[*strong.values()], color="r", width=0.4, label="Weak choices")
    plt.xlabel("Final decision")
    plt.ylabel("Number of choices")
    plt.yticks(range(0, 9, 1))
    plt.title(description)
    plt.legend()
    # plt.savefig("images/" + filename.split("/")[1].split(".")[0] + ".png")
    plt.show()

# taking cloud and local summaries separately
cloud_summary = {level: mappings["Cloud"] for level, mappings in all_decisions_occurrences.items()}
local_summary = {level: mappings["Local"] for level, mappings in all_decisions_occurrences.items()}

# plotting histogram
plt.bar([*cloud_summary.keys()], [*cloud_summary.values()], color="b", width=0.25, label="Cloud")
plt.bar([x + 0.25 for x in local_summary.keys()], [*local_summary.values()], color="y", width=0.25, label="Local")
plt.xlabel("Security level")
plt.ylabel("Number of choices")
plt.yticks(range(0, 13, 1))
plt.title("Summary from all experiments")
plt.legend()
plt.savefig("images/exp_histogram.png")
plt.show()

from trial import Trial, MyException
import json

trials = []

with open('../data.json') as json_data:
    d = json.load(json_data)

    for obj in d["data"]:

        try:
            t = Trial(obj)
        except MyException:
            continue 

        t.clean()
        trials.append(t)




done = []
done_obj = []
for t in trials:
    if t.participant_id not in done:
        done_obj.append(t)
        done.append(t.participant_id)

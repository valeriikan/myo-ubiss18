from trial import Trial, MyException
import json
import os

trials = []

folder = os.path.dirname(os.path.realpath(__file__))

with open(folder + '/../data.json') as json_data:
    d = json.load(json_data)

    for obj in d["data"]:

        try:
            t = Trial(obj)
            print "!",t.alcohol.units
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


print [d.gender for d in done_obj]
print len(trials)
print [t.y() for t in trials]
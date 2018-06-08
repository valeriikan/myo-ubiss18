from data import trials
from svm import model
import numpy as np
from sklearn.metrics import mean_absolute_error, mean_squared_error

diffs = []
preds = []
reals = []
for t in trials:
	all_other = [t2 for t2 in trials if t2 is not t]

	clf = model(all_other)

	pred = clf.predict(np.array(t.X()).reshape(1, -1))[0]

	if pred < 0:
		pred = 0.0

	real = t.y()

	diff = abs(pred - real)

	print "pred",pred,"real",real

	diffs.append(diff)

	preds.append(pred)
	reals.append(real)


print np.mean(diffs)


print "MAE", mean_absolute_error(preds, reals)
print "MSE", mean_squared_error(preds, reals)

from trial import Participant


participants = [Participant(), Participant()]


for p in participants:
	for trial in p.trials:
		X = trial.X()
		y = trial.y()

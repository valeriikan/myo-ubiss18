import json
from feature import Feature

class MyException(Exception):
	pass

class Alchohol():
	units = 0
	
	# e.g., 19.5, and 21.0
	start_hours = 0
	end_hours = 0


class Participant():
	trial1 = None
	trial2 = None
	trial3 = None
	trial4 = None

	def __init__(self):
		self.trial1 = Trial(1)
		self.trial2 = Trial(2)
		self.trial3 = Trial(3)
		self.trial4 = Trial(4)

	def trials(self):
		return [self.trial1, self.trial2, self.trial3, self.trial4]


class Trial():
	participant_id = None
	
	# 1,2,3
	trial = None

	# "male" or "female"
	gender = None

	# in kilos
	weight = None
	
	# user input
	alcohol = None

	# time series
	emg0 = None
	emg1 = None
	emg2 = None
	emg3 = None
	emg4 = None
	emg5 = None
	emg6 = None
	emg7 = None

	# time series
	accX = None
	accY = None
	accZ = None

	# time series
	gyroX = None
	gyroY = None
	gyroZ = None

	# in ms (long)
	start_time = None
	end_time = None

	# arm / leg
	place_of_myo = None


	def __init__(self, jsondata):
		self.alcohol = Alchohol()

		p = json.loads(jsondata["participant"])


		try:
			self.participant_id = int(p["participant_id"])
		except:
			raise MyException()

		try:
			self.alcohol.start_hours = int(p["drink_start_time"][0:2]) + float("0."+p["drink_start_time"][2:])
		except:
			self.alcohol.start_hours = 15
	

		try:
			self.trial = int(p["trial"])
			self.alcohol.units = int(p["number_of_bottles"][0:1]) + float("0."+p["number_of_bottles"][1:])

			self.gender = p["gender"].lower()
			self.weight = int(p["weight"][0:2]) + float("0."+p["weight"][2:])

			self.alcohol.end_hours = int(p["trial_time"])

			self.start_time = int(p["trial_start_time"])
			self.end_time = int(jsondata["trial_end_time"])
		except:
			# prediction
			pass


		acc = json.loads(jsondata["acc"])
		gyro = json.loads(jsondata["gyro"])

		self.accX = [d["x"] for d in acc]
		self.accY = [d["y"] for d in acc]
		self.accZ = [d["z"] for d in acc]
		self.accT = [d["t"] for d in acc]

		self.gyroX = [d["x"] for d in gyro]
		self.gyroY = [d["y"] for d in gyro]
		self.gyroZ = [d["z"] for d in gyro]
		self.gyroT = [d["t"] for d in gyro]


	def clean(self):
		t0 = self.accT[0]
		t1 = self.accT[len(self.accT)/10]
		th = self.accT[len(self.accT)/3]


		self.accX = [x for x,t in zip(self.accX,self.accT) if t > t1 and t < th]
		self.accY = [x for x,t in zip(self.accY,self.accT) if t > t1 and t < th]
		self.accZ = [x for x,t in zip(self.accZ,self.accT) if t > t1 and t < th]

		self.gyroX = [x for x,t in zip(self.accX,self.accT) if t > t1 and t < th]
		self.gyroY = [x for x,t in zip(self.accY,self.accT) if t > t1 and t < th]
		self.gyroZ = [x for x,t in zip(self.accZ,self.accT) if t > t1 and t < th]


	# BW is a body water constant (0.58 for males and 0.49 for females)
	def BW(self):
		if self.gender == "male":
			return .58

		return .49

	# MR is the metabolism constant (0.015 for males and 0.017 for females) and
	def MR(self):
		if self.gender == "male":
			return .015

		return .017


	# https://en.wikipedia.org/wiki/Blood_alcohol_content
	# g/dL alcohol
	def ebac(self):
		SD = self.alcohol.units
		BW = self.BW()
		Wt = self.weight
		MR = self.MR()
		DP = self.alcohol.end_hours - self.alcohol.start_hours

		#print "SD",SD,"BW",BW,"Wt",Wt,"MR",MR,"DP",DP


		return 10*((.806 * SD * 1.2) / (BW * Wt) - (MR * DP))


	def X(self):
		f = Feature()

		return f.matrix(self)


	def y(self):
		return self.ebac()


if __name__ == "__main__":
	# small test, values taken from wiki

	test = Trial(None)
	test.weight = 80
	test.alchohol.beers = 3
	test.start = 0
	test.end = 2
	test.gender = "male"
	
	assert test.ebac() - 0.0325344827586 < .00001

	test = Trial(None)
	test.weight = 70
	test.alchohol.beers = 2.5
	test.start = 0
	test.end = 2
	test.gender = "female"

	assert test.ebac() - 0.0364956268222 < .00001

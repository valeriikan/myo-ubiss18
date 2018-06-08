import numpy as np
from statsmodels.tsa.arima_model import ARMA
import math


class Feature():
    trial = None

    _matrix = None

    def __init__(self):
        self._matrix = []

    def data(self):
        return {
            "acc": {
                "x": self.trial.accX,
                "y": self.trial.accY,
                "z": self.trial.accZ,
                "t": self.trial.accT,
            },
            "gyro": {
                "x": self.trial.gyroX,
                "y": self.trial.gyroY,
                "z": self.trial.gyroZ,
                "t": self.trial.accT,
            },
            "emg0": [],
            "emg1": []
        }

    def descriptive_motion(self, sensor, axis):
        d = self.data()[sensor][axis]

        # rm nan
        d = [value for value in d if not math.isnan(value)]

        energy = sum([x**2 for x in d]) / float(len(d))
        correlation = np.cov(d) / (np.std(d)**2)

        return np.mean(d), np.max(d), np.std(d), energy, correlation

    def arma(self, sensor, axis):
        signal = self.data()[sensor][axis]

        mod = ARMA(signal, order=(2,0))
        res = mod.fit(disp=False)

        return res.arparams


    def matrix(self, trial):
        self.trial = trial

        for sensor in ["acc"]:
            for axis in ["x","y","z"]:
                for d in self.arma(sensor, axis):
                    self._matrix.append(d)

        # for sensor in ["gyro"]:
        #     for axis in ["x","y","z"]:
        #         f1,f2,f3,f4,f5 = self.descriptive_motion(sensor, axis)
        #         self._matrix.append(f1)
        #         self._matrix.append(f2)
        #         self._matrix.append(f3)
        #         self._matrix.append(f4)
        #         self._matrix.append(f5)


        return self._matrix




if __name__ == "__main__":
    f = Feature(1)



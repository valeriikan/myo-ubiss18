import numpy as np
# from sklearn import svm
# from sklearn.model_selection import GridSearchCV
# from sklearn import linear_model

from feature import Feature
import statsmodels.api as sm

parameters = {'kernel': ('rbf','poly'), 'C':[1, 10, 100, 1000, 10000],'gamma': [1e-7, 1e-4, 1e-2, 1] ,'epsilon':[0.1,0.2,0.3,0.5,0.8]}



def model(trials):
	#svr = svm.SVR()
	#clf = GridSearchCV(svr, parameters)

	#clf = linear_model.LinearRegression()

	Xs =[
		p.X() for p in trials
	]

	ys = [p.y() for p in trials]

	#clf.fit(Xs, ys)

	clf = sm.OLS(ys, Xs).fit()
		

	return clf 

